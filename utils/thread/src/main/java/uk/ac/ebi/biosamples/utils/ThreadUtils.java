package uk.ac.ebi.biosamples.utils;

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

	private static Logger log = LoggerFactory.getLogger(ThreadUtils.class);
	
	public static <T> void checkFutures(Map<? extends Object, Future<T>> futures, int maxSize) throws InterruptedException,ExecutionException {
		while (futures.size() > maxSize) {
			for (Iterator<? extends Object> i = futures.keySet().iterator(); i.hasNext(); ) {
				Object key = i.next();
				futures.get(key).get();
				i.remove();
			}
		}
	}
	
	public static <T> void checkAndCallbackFutures(Map<? extends Object, Future<T>> futures, int maxSize, Callback<T> callback) throws InterruptedException,ExecutionException {
		while (futures.size() > maxSize) {
			for (Iterator<? extends Object> i = futures.keySet().iterator(); i.hasNext(); ) {
				Object key = i.next();
				callback.call(futures.get(key).get());
				i.remove();
			}
		}
	}

	public static interface Callback<T> {
		public void call(T t);
	}
	
	public static <T,U> void checkAndRetryFutures(Map<T, Future<U>> futures, Map<T, Callable<U>> callables,
			int maxSize, ExecutorService executionService) throws InterruptedException{
		while (futures.size() > maxSize) {
			List<T> toReRun = new ArrayList<>();
			for (Iterator<T> i = futures.keySet().iterator(); i.hasNext(); ) {
				T key = i.next();
				try {
					futures.get(key).get();
				} catch (ExecutionException e) {
					toReRun.add(key);
				}
				i.remove();
			}
			for (T key : toReRun) {
				log.info("Re-executing "+key);
				futures.put(key, executionService.submit(callables.get(key)));
			}
		}
	}
}
