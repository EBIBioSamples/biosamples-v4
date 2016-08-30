package uk.ac.ebi.biosamples.utils;

import java.util.HashMap;
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

	public static AdaptiveThreadPoolExecutor create() {
		return create(1000,60000,false);
	}

	public static AdaptiveThreadPoolExecutor create(int maxQueueSize, int pollInterval, boolean fairness) {

		//default to the number of processors
		int corePoolSize =  Runtime.getRuntime().availableProcessors();
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

	private static class PoolMonitor implements Runnable {
		
	    private Logger log = LoggerFactory.getLogger(this.getClass());

		private final AdaptiveThreadPoolExecutor pool;
		private final int pollInterval;
		private final Map<Integer, Double> doneScores = new HashMap<>();
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
				
				double score = (((double)doneJobs)/interval)*1000000000.0d;
				

				
				log.info("Completed "+doneJobs+" in "+interval+"ns using "+currentThreads+" threads");
				
				doneScores.put(currentThreads, score);
				
				//see if we might do better increase or decreasing the threads				
				if (!doneScores.containsKey(currentThreads+1) || doneScores.get(currentThreads+1) > margin*doneJobs) {
					log.info("Adjusting to use "+(currentThreads+1)+" threads");
					//increase the number of threads
					pool.setMaximumPoolSize(currentThreads+1);
					pool.setCorePoolSize(currentThreads+1);
				} else  if (!doneScores.containsKey(currentThreads-1) || doneScores.get(currentThreads-1) > margin*doneJobs) {
					//decrease the number of threads
					pool.setMaximumPoolSize(currentThreads-1);
					pool.setCorePoolSize(currentThreads-1);
					log.info("Adjusting to use "+(currentThreads-1)+" threads");
				}
			}
		}
	}
}
