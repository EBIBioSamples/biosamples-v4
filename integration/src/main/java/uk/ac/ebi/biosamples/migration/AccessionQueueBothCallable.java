package uk.ac.ebi.biosamples.migration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class AccessionQueueBothCallable implements Callable<Void> {
	
	private final Queue<String> oldQueue;
	private final Set<String> oldSet = new HashSet<>();
	private final AtomicBoolean oldFlag;
	private final Queue<String> newQueue;
	private final Set<String> newSet = new HashSet<>();
	private final AtomicBoolean newFlag;
	private final Queue<String> bothQueue;
	private final AtomicBoolean bothFlag;

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public AccessionQueueBothCallable(Queue<String> oldQueue, AtomicBoolean oldFlag, 
			Queue<String> newQueue, AtomicBoolean newFlag,
			Queue<String> bothQueue, AtomicBoolean bothFlag) {
		this.oldQueue = oldQueue;
		this.oldFlag = oldFlag;
		this.newQueue = newQueue;
		this.newFlag = newFlag;
		this.bothQueue = bothQueue;
		this.bothFlag = bothFlag;
	}
	
	
	
	@Override
	public Void call() throws Exception {
		log.info("Started AccessionQueueBothCallable.call()");
		
		//first, pre-load those we already know are problematic
		Set<String> toIgnore = new HashSet<>();
		
		try (BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/accession_ignore.txt")))) {
			String line = null;
			while ((line=br.readLine()) != null) {
				String next = line.trim();
				//lines with # are comments
				if (!next.startsWith("#")) {
					toIgnore.add(next);
					log.trace("ignoring "+next);
				}
			}
		}
		
		log.info("read accessions to ignore");

		while (!oldFlag.get() || !oldQueue.isEmpty() || !newFlag.get() || !newQueue.isEmpty()) {
			if (!oldFlag.get() || !oldQueue.isEmpty()) {
				String next = oldQueue.poll();
				if (next != null && !toIgnore.contains(next)) {
					oldSet.add(next);
					if (newSet.contains(next)) {
						while (!bothQueue.offer(next)) {
							Thread.sleep(100);
						}
					}
				}
			}
			if (!newFlag.get() || !newQueue.isEmpty()) {
				String next = newQueue.poll();
				if (next != null && !toIgnore.contains(next)) {
					newSet.add(next);
					if (oldSet.contains(next)) {
						while (!bothQueue.offer(next)) {
							Thread.sleep(100);
						}
					}
				}
			}
		}
		
		//at his point we should be able to generate the differences in the sets
		
		Set<String> newOnly = Sets.difference(newSet, oldSet);
		Set<String> oldOnly = Sets.difference(oldSet, newSet);
		log.info("Samples only in new "+newOnly.size());
		log.info("Samples only in old "+oldOnly.size());

		int i;
		Iterator<String> accIt;
		
		accIt = newOnly.iterator();
		i = 0;
		while (accIt.hasNext()) {
			log.info("Sample only in new "+accIt.next());
			i++;
		}
		
		accIt = oldOnly.iterator();
		i = 0;
		while (accIt.hasNext()) {
			log.info("Sample only in old "+accIt.next());
			i++;
		}
		 
		
		bothFlag.set(true);
		log.info("Finished AccessionQueueBothCallable.call(");
		
		return null;
	}
	
}
