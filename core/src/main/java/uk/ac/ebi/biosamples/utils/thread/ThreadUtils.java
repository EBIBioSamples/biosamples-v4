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
package uk.ac.ebi.biosamples.utils.thread;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadUtils {
  private static final Logger log = LoggerFactory.getLogger(ThreadUtils.class);

  public static <T> void checkFutures(
      final Map<? extends Object, Future<T>> futures, final int maxSize)
      throws InterruptedException, ExecutionException {

    while (futures.size() > maxSize) {
      for (final Iterator<? extends Object> i = futures.keySet().iterator(); i.hasNext(); ) {
        final Object key = i.next();

        futures.get(key).get();
        i.remove();
      }
    }
  }

  public static <T> void checkAndCallbackFutures(
      final Map<? extends Object, Future<T>> futures, final int maxSize, final Callback<T> callback)
      throws InterruptedException, ExecutionException {
    while (futures.size() > maxSize) {
      for (final Iterator<? extends Object> i = futures.keySet().iterator(); i.hasNext(); ) {
        final Object key = i.next();

        callback.call(futures.get(key).get());
        i.remove();
      }
    }
  }

  public interface Callback<T> {
    void call(T t);
  }

  public static <T, U> void checkAndRetryFutures(
      final Map<T, Future<U>> futures,
      final Map<T, Callable<U>> callables,
      final int maxSize,
      final ExecutorService executionService)
      throws InterruptedException {
    while (futures.size() > maxSize) {
      final List<T> toReRun = new ArrayList<>();

      for (final Iterator<T> i = futures.keySet().iterator(); i.hasNext(); ) {
        final T key = i.next();

        try {
          futures.get(key).get();
        } catch (final ExecutionException e) {
          toReRun.add(key);
        }
        i.remove();
      }

      for (final T key : toReRun) {
        log.info("Re-executing " + key);
        futures.put(key, executionService.submit(callables.get(key)));
      }
    }
  }
}
