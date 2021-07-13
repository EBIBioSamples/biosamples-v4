/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
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

public class AdaptiveThreadPoolExecutor extends ThreadPoolExecutor implements AutoCloseable {
  private Logger log = LoggerFactory.getLogger(this.getClass());
  private AtomicInteger completedJobs = new AtomicInteger(0);

  private AdaptiveThreadPoolExecutor(
      int corePoolSize,
      int maximumPoolSize,
      long keepAliveTime,
      TimeUnit unit,
      BlockingQueue<Runnable> workQueue,
      RejectedExecutionHandler rejectedExecutionHandler) {

    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, rejectedExecutionHandler);
  }

  protected void afterExecute(Runnable r, Throwable t) {
    if (t != null) return;

    completedJobs.incrementAndGet();
  }

  /**
   * This is required to implement the AutoClosable interface. It will stop accepting new jobs and
   * wait up to 24h before termination;
   */
  @Override
  public void close() throws Exception {
    shutdown();
    awaitTermination(1, TimeUnit.MINUTES);
  }

  /**
   * By default creates a pool with a queue size of 1000 that will test to increase/decrease threads
   * every 60 seconds and does not guarantee to distribute jobs fairly among threads
   *
   * @return
   */
  public static AdaptiveThreadPoolExecutor create() {
    return create(1000, 60000, false);
  }

  public static AdaptiveThreadPoolExecutor create(
      int maxQueueSize, int pollInterval, boolean fairness) {
    return create(
        maxQueueSize,
        pollInterval,
        fairness,
        Runtime.getRuntime().availableProcessors(),
        Runtime.getRuntime().availableProcessors() * 8);
  }

  public static AdaptiveThreadPoolExecutor create(
      int maxQueueSize, int pollInterval, boolean fairness, int initialPoolSize, int maxThreads) {
    // default to the number of processors
    int corePoolSize = initialPoolSize;
    int maximumPoolSize = corePoolSize;
    // keep alive is not relevant, since core == maximum
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
    AdaptiveThreadPoolExecutor threadPool =
        new AdaptiveThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            unit,
            workQueue,
            rejectedExecutionHandler);
    Thread monitorThread = new Thread(new PoolMonitor(threadPool, pollInterval, maxThreads));

    monitorThread.setDaemon(true);
    monitorThread.start();

    return threadPool;
  }

  /**
   * This is a separate thread that monitors a thread pool and increases or decreases the number of
   * threads within the pool in order to try to maximize the throughput.
   *
   * @author faulcon
   */
  private static class PoolMonitor implements Runnable {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private final AdaptiveThreadPoolExecutor pool;
    private final int pollInterval;
    private final Map<Integer, Double> threadsScores = new HashMap<>();
    private final Map<Integer, Long> threadsTime = new HashMap<>();
    private final double margin = 1.0;
    private final int maxThreads;

    public PoolMonitor(AdaptiveThreadPoolExecutor pool, int pollInterval, int maxThreads) {
      this.pool = pool;
      this.pollInterval = pollInterval;
      this.maxThreads = maxThreads;
    }

    @Override
    public void run() {
      long lastStep = System.nanoTime();

      while (!pool.isTerminated()) {
        // wait for it to do stuff
        try {
          Thread.sleep(pollInterval);
        } catch (InterruptedException e) {
          if (Thread.interrupted()) { // Clears interrupted status!
            throw new RuntimeException(e);
          }
        }

        // test the number of jobs done
        // get number of threads they were done with

        long now = System.nanoTime();
        long interval = now - lastStep;
        lastStep = now;

        int currentThreads = pool.getMaximumPoolSize();
        int doneJobs = pool.completedJobs.getAndSet(0);
        // number of jobs per sec
        double score = (((double) doneJobs) * 1000000000.0d) / (interval);

        log.trace(
            "Completed "
                + doneJobs
                + " in "
                + interval
                + "ns using "
                + currentThreads
                + " threads : score = "
                + score);
        // store the result of this score
        threadsScores.put(currentThreads, score);
        threadsTime.put(currentThreads, now);
        // remove any scores that are too old
        Iterator<Integer> iterator = threadsTime.keySet().iterator();

        while (iterator.hasNext()) {
          int testThreads = iterator.next();
          long testTime = threadsTime.get(testThreads);
          // more than 25 pollings ago?
          if (testTime + (pollInterval * 1000000l * 25) < now) {
            // too old score, remove it
            log.trace(
                "Remove out-of-date score for "
                    + testThreads
                    + " of "
                    + threadsScores.get(testThreads));
            iterator.remove();
            threadsScores.remove(testThreads);
          }
        }

        // work out what the best number of threads is
        double bestScore = score;
        int bestThreads = currentThreads;

        for (int testThreads : threadsScores.keySet()) {
          double testScore = threadsScores.get(testThreads);

          if (testScore > bestScore) {
            bestScore = testScore;
            bestThreads = testThreads;
          }
        }
        log.trace("Best scoring number of threads is " + bestThreads + " with " + bestScore);

        // if we are more than margin below the best, change to the best
        if (bestThreads != currentThreads && margin * score < bestScore) {
          log.trace("Adjusting to use " + (bestThreads) + " threads");
          setPoolSizesCoreFirst(bestThreads);
        } else {
          // experiment if we might do better increase or decreasing the threads
          if ((!threadsScores.containsKey(currentThreads + 1)
                  || threadsScores.get(currentThreads + 1) > margin * score)
              && currentThreads < maxThreads) {
            // increase the number of threads
            log.trace("Adjusting to try " + (currentThreads + 1) + " threads");
            setPoolSizesMaxFirst(currentThreads + 1);
          } else if (currentThreads > 1
              && (!threadsScores.containsKey(currentThreads - 1)
                  || threadsScores.get(currentThreads - 1) > margin * score)) {
            // decrease the number of threads
            // only decrease threads if there are at least 2 (so we don't drop to zero!)
            log.trace("Adjusting to try " + (currentThreads - 1) + " threads");
            setPoolSizesCoreFirst(currentThreads - 1);
          }
        }
      }
    }

    private void setPoolSizesCoreFirst(int bestThreads) {
      try {
        pool.setCorePoolSize(bestThreads);
        pool.setMaximumPoolSize(bestThreads);
      } catch (final IllegalArgumentException illPoolSizes) {
        pool.setMaximumPoolSize(pool.getCorePoolSize() + 1);
      }
    }

    private void setPoolSizesMaxFirst(int bestThreads) {
      try {
        pool.setMaximumPoolSize(bestThreads);
        pool.setCorePoolSize(bestThreads);
      } catch (final IllegalArgumentException illPoolSizes) {
        pool.setMaximumPoolSize(pool.getCorePoolSize() + 1);
      }
    }
  }
}
