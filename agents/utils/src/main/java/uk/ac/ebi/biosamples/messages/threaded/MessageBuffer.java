package uk.ac.ebi.biosamples.messages.threaded;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.scheduling.annotation.Scheduled;

public abstract class MessageBuffer<S, T extends CrudRepository<S,?>> {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final T repository;	
	
    private final BlockingQueue<MessageSampleStatus<S>> messageSampleStatusQueue;
    private final AtomicLong latestTime;
    public final AtomicBoolean hadProblem = new AtomicBoolean(false);
 
    static final int QUEUE_SIZE = 1000;
    static final int MAX_WAIT = 1000;
	
	public MessageBuffer(@Autowired T repository) {
		this.repository = repository;
		messageSampleStatusQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);
		latestTime = new AtomicLong(0);
	}
	
	public MessageSampleStatus<S> recieve(S sample) throws InterruptedException {
		//if there is no maximum time
		//set it to wait in the future
		latestTime.compareAndSet(0, Instant.now().toEpochMilli()+MAX_WAIT);
		
		MessageSampleStatus<S> status = MessageSampleStatus.build(sample);
		
		//this will block until space is available
		messageSampleStatusQueue.put(status);
		return status;
	}

	
	@Scheduled(fixedDelay = 100)
	public void checkQueueStatus() {
		//check if enough time has elapsed
		//or if the queue is long enough
		int remaining = messageSampleStatusQueue.remainingCapacity();
		long now = Instant.now().toEpochMilli();
		long latestTimeLong = latestTime.get();
		log.trace(""+remaining+" queue spaces and now "+now+" vs "+latestTimeLong);
		if (remaining <= 0.01*QUEUE_SIZE
				|| (now > latestTimeLong && latestTimeLong != 0)) {

			if (remaining <= 0.01*QUEUE_SIZE) {
				log.info("Committing queue because full");
			}
			if (now > latestTimeLong && latestTimeLong != 0) {
				log.info("Committing queue because old");
			}
			
			
			//create a local collection of the messages
			List<MessageSampleStatus<S>> messageSampleStatuses = new ArrayList<>(QUEUE_SIZE);
			
			try {
				//unset the latest time so that it can be set again by the next message
				latestTime.set(0);
				
				//drain the master queue into it
				messageSampleStatusQueue.drainTo(messageSampleStatuses, QUEUE_SIZE);					
				//now we can process the local copy without worrying about new ones being added
				
				//split out the samples into a separate list
				List<S> samples = new ArrayList<>(messageSampleStatuses.size());
				messageSampleStatuses.stream().forEach(m -> samples.add(m.sample));
		
				//send everything as a single transaction
				save(repository, samples);
				//this was a hard commit so they are now written to disk
				
				//if there was a problem, an exception would be thrown
				//since we are still here, no unrecoverable problems occurred
				
				//mark each of the status as completed
				//this will trigger the waiting threads to continue
				messageSampleStatuses.stream().forEach(m -> m.storedInRepository.set(true));
			} catch (RuntimeException e) {
				//store that we encountered a problem of some kind
				log.error("Storing warning", e);
				hadProblem.set(true);
				messageSampleStatuses.stream().forEach(m -> m.hadProblem.set(e,true));
				//re-throw the exception
				throw e;
			}
			
		}
	}
	/**
	 * This is the method that a specific sub-class should implement. Typically
	 * this will be some sort of repository.save(samples) call in a transaction
	 * 	
	 * @param repository
	 * @param samples
	 */
	protected abstract void save(T repository, Collection<S> samples);
}
