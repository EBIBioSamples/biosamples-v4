package uk.ac.ebi.biosamples;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.biosamples.models.JPAAttribute;
import uk.ac.ebi.biosamples.models.JPASample;
import uk.ac.ebi.biosamples.models.SimpleSample;
import uk.ac.ebi.biosamples.repos.JPAAttributeRepository;
import uk.ac.ebi.biosamples.repos.JPASampleRepository;

@Component
public class Runner implements ApplicationRunner {

	private Logger log = LoggerFactory.getLogger(getClass());

	private RabbitMessagingTemplate rabbitTemplate;

	@Autowired
	private JPASampleRepository jpaSampleRepository;

	@Autowired
	private JPAAttributeRepository jpaAttributeRepository;

	public Runner(RabbitMessagingTemplate rabbitTemplate, MessageConverter messageConverter) {
		this.rabbitTemplate = rabbitTemplate;
		this.rabbitTemplate.setMessageConverter(messageConverter);
	}

	@Transactional
	public void recieveForLoading(SimpleSample sample) throws Exception {
		log.info("Recieved " + sample.getAccession());
		// convert it to the right object type
		JPASample jpaSample = JPASample.createFrom(sample);

		JPASample oldSample = jpaSampleRepository.findByAccession(jpaSample.getAccession());
		if (oldSample != null) {
			jpaSample.setId(oldSample.getId());
			log.info("Updating sample " + sample.getAccession() + " (" + oldSample.getId() + ")");
		} else {
			// save the new one
			log.info("Saving sample " + sample.getAccession());
		}

		// for each attribute, get lowest existing ids (if any)
		for (JPAAttribute attribute : jpaSample.getAttributes()) {
			
			String type = attribute.getKey();
			String value = attribute.getValue();
			String unit = attribute.getUnit();
			String ontologyTerm = attribute.getOntologyTerm();
			
			Long oldAttributeId = null;
			
			//TODO check old sample if present
			
			Iterable<JPAAttribute> oldAttributes = jpaAttributeRepository.findByTypeAndValueAndUnitAndOntologyTerm(type, value, unit, ontologyTerm);
			
			for(JPAAttribute oldAttribute : oldAttributes) {
				if (oldAttributeId == null || oldAttribute.getId() < oldAttributeId) {
					oldAttributeId = oldAttribute.getId();
				}
			}
			
			if (oldAttributeId != null) {
				log.info("Got old attribute ("+oldAttributeId+") for "+type+" "+value+" "+unit+" "+ontologyTerm);
				attribute.setId(oldAttributeId);
			}
			jpaAttributeRepository.save(attribute);
		}

		//TODO relationships
		
		jpaSample = jpaSampleRepository.save(jpaSample);
	}

	private class RunRabbit implements Runnable {

		public AtomicBoolean busy = new AtomicBoolean(true);

		public AtomicBoolean toDie = new AtomicBoolean(false);

		public RunRabbit() {
		}

		@Override
		public void run() {
			SimpleSample sample;
			while (!toDie.get()) {
				sample = rabbitTemplate.receiveAndConvert(Messaging.queueToBeLoaded, SimpleSample.class);
				if (sample != null) {
					busy.set(true);
					try {
						recieveForLoading(sample);
					} catch (Exception e) {
						//problem processing it, put it back on the queue?
						rabbitTemplate.convertAndSend(Messaging.queueToBeLoaded, sample);
					}
				} else {
					busy.set(false);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}

	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Starting...");

		int noThreads = 1;
		if (args.containsOption("threads")) {
			noThreads = Integer.parseInt(args.getOptionValues("threads").iterator().next());
		}

		List<Thread> threads = new LinkedList<>();
		List<RunRabbit> runRabbits = new LinkedList<>();
		for (int i = 0; i < noThreads; i++) {
			RunRabbit r = new RunRabbit();
			runRabbits.add(r);
			Thread t = new Thread(r);
			t.start();
			threads.add(t);
		}

		boolean processing = true;
		while (processing) {
			// see how many threads are busy
			int busyThreads = 0;
			for (int i = 0; i < threads.size(); i++) {
				if (runRabbits.get(i).busy.get() && threads.get(i).isAlive()) {
					busyThreads += 1;
				} 
			}
			log.info("Number of busy threads = " + busyThreads);
			// if no threads are busy, decide if we are ending
			if (busyThreads == 0 && !args.containsOption("always")) {
				processing = false;
				for (int i = 0; i < threads.size(); i++) {
					Thread t = threads.get(i);
					RunRabbit r = runRabbits.get(i);
					log.info("Stopping thread " + t);
					r.toDie.set(true);
					t.join();
				}
			} else {
				// check if any threads are dead (crashed)
				for (int i = 0; i < threads.size(); i++) {
					Thread t = threads.get(i);
					if (!t.isAlive()) {
						// restart it
						//log.warn("Thread died, restarting...");
						//RunRabbit r = new RunRabbit();
						//runRabbits.set(i, r);
						//t = new Thread(r);
						//t.start();
						//threads.set(i, t);
					}
				}
				// take a nap
				Thread.sleep(1000);
			}
		}
		log.info("Exiting ...");
	}

}
