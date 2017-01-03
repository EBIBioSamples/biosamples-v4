package uk.ac.ebi.biosamples.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdaptiveThreadPoolExecutor extends ThreadPoolExecutor {
	
    private Logger log = LoggerFactory.getLogger(this.getClass());

	private AtomicInteger completedJobs = new AtomicInteger(0);
	
	private AdaptiveThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, RejectedExecutionHandler rejectedExecutionHandler) {

		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, rejectedExecutionHandler);
	}
	
	
	protected void afterExecute(Runnable r, Throwable t) {
		if (t != null) return;
		completedJobs.incrementAndGet();
	}

	/**
	 * By default creates a pool with a queue size of 1000 that 
	 * will test to increase/decrease threads every 60 seconds
	 * and does not guarantee to distribute jobs fairly amoung threads
	 * @return
	 */
	public static AdaptiveThreadPoolExecutor create() {
		return create(1000,60000,false);
	}

	public static AdaptiveThreadPoolExecutor create(int maxQueueSize, int pollInterval, boolean fairness) {
		return create(maxQueueSize, pollInterval, fairness, Runtime.getRuntime().availableProcessors());
	}

	public static AdaptiveThreadPoolExecutor create(int maxQueueSize, int pollInterval, boolean fairness, int initialPoolSize) {

		//default to the number of processors
		int corePoolSize = initialPoolSize;
		int maximumPoolSize = corePoolSize;
		
		//keep alive is not relevant, since core == maximum
		long keepAliveTime = 1;
		TimeUnit unit = TimeUnit.DAYS;

		// a queue constructed with fairness set to true grants threads access
		// in FIFO order.
		// Fairness generally decreases throughput but reduces variability and
		// avoids starvation.
		BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(maxQueueSize, fairness);

		// A handler for rejected tasks that runs the rejected task directly in
		// the calling thread of the execute method,
		// unless the executor has been shut down, in which case the task is
		// discarded.
		RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();

		AdaptiveThreadPoolExecutor threadPool = new AdaptiveThreadPoolExecutor(corePoolSize, maximumPoolSize,
				keepAliveTime, unit, workQueue, rejectedExecutionHandler);
		
		Thread monitorThread = new Thread(new PoolMonitor(threadPool, pollInterval));
		monitorThread.setDaemon(true);
		monitorThread.start();
		
		return threadPool;
	}

	/**
	 * This is a separate thread that monitors a thread pool
	 * and increases or decreases the number of threads within the pool
	 * in order to try to maximize the throughput. 
	 * @author faulcon
	 *
	 */
	private static class PoolMonitor implements Runnable {
		
	    private Logger log = LoggerFactory.getLogger(this.getClass());

		private final AdaptiveThreadPoolExecutor pool;
		private final int pollInterval;
		private final Map<Integer, Double> threadsScores = new HashMap<>();
		private final Map<Integer, Long> threadsTime = new HashMap<>();
		private final double margin = 1.1;

		public PoolMonitor(AdaptiveThreadPoolExecutor pool, int pollInterval) {
			this.pool = pool;
			this.pollInterval = pollInterval;
		}

		@Override
		public void run() {
			long lastStep = System.nanoTime();
			while (!pool.isTerminated()) {
				//wait for it to do stuff
				try {
					Thread.sleep(pollInterval);
				} catch (InterruptedException e) {
					if (Thread.interrupted())  {// Clears interrupted status!
						throw new RuntimeException(e);
					}
				}
				
				//test the number of jobs done
				//get number of threads they were done with

				long now = System.nanoTime();
				long interval = now-lastStep;
				lastStep = now;
				
				int currentThreads = pool.getMaximumPoolSize();
				int doneJobs = pool.completedJobs.getAndSet(0);
				
				//number of jobs per sec per thread
				double score = (((double)doneJobs)*1000000000.0d)/(interval*currentThreads);
				
				log.trace("Completed "+doneJobs+" in "+interval+"ns using "+currentThreads+" threads : score = "+score);
				
								
				//store the result of this score
				threadsScores.put(currentThreads, score);
				threadsTime.put(currentThreads, now);
				
				//remove any scores that are too old
				Iterator<Integer> iterator = threadsTime.keySet().iterator();
				while (iterator.hasNext()) {
					int testThreads = iterator.next();
					long testTime = threadsTime.get(testThreads);
					//more than 10 pollings ago?
					if (testTime + (pollInterval*1000000l*10) < now) {
						//too old score, remove it
						log.info("Remove out-of-date score for "+testThreads);
						iterator.remove();
						threadsTime.remove(testThreads);
					}
				}
				
				//work out what the best number of threads is
				double bestScore = score;
				int bestThreads = currentThreads;
				for (int testThreads : threadsScores.keySet()) {
					double testScore = threadsScores.get(testThreads); 
					if (testScore > bestScore) {
						bestScore = testScore;
						bestThreads = testThreads;
					}
				}
				log.trace("Best scoring number of threads is "+bestThreads+" with "+bestScore);
								
				//if we are more than margin below the best, change to the best
				if (bestThreads != currentThreads && margin*score < bestScore) {	
					log.info("Adjusting to use "+(bestThreads)+" threads");
					pool.setCorePoolSize(bestThreads);
					pool.setMaximumPoolSize(bestThreads);
				} else {
					//experiment if we might do better increase or decreasing the threads	
					if (!threadsScores.containsKey(currentThreads+1) || threadsScores.get(currentThreads+1) > margin*score) {
						//increase the number of threads			
						log.info("Adjusting to try "+(currentThreads+1)+" threads");
						pool.setCorePoolSize(currentThreads+1);
						pool.setMaximumPoolSize(currentThreads+1);
					} else if (currentThreads > 1 && (!threadsScores.containsKey(currentThreads-1) || threadsScores.get(currentThreads-1) > margin*score)) {
						//decrease the number of threads
						//only decrease threads if there are at least 2 (so we don't drop to zero!)
						log.info("Adjusting to try "+(currentThreads-1)+" threads");
						pool.setCorePoolSize(currentThreads-1);
						pool.setMaximumPoolSize(currentThreads-1);
					}
				}
			}
		}
	}
}
