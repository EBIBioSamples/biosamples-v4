package uk.ac.ebi.biosamples.utils;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadUtils {

	private static Logger log = LoggerFactory.getLogger(ThreadUtils.class);
	
	public static void checkFutures(Map<? extends Object, Future<Void>> futures, int maxSize) throws InterruptedException {
		while (futures.size() > maxSize) {
			for (Iterator<? extends Object> i = futures.keySet().iterator(); i.hasNext(); ) {
				Object key = i.next();
				try {
					futures.get(key).get();
				} catch (ExecutionException e) {
					log.error("Error processing "+key, e);
				}
				i.remove();
			}
		}
	}
}
