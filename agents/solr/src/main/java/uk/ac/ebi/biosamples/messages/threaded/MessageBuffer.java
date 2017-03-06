package uk.ac.ebi.biosamples.messages.threaded;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

@Service
public class MessageBuffer {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final SolrSampleRepository solrSampleRepository;
	
	
    private final BlockingQueue<MessageSampleStatus> messageSampleStatusQueue;
    private final AtomicLong latestTime;
 
    static final int QUEUE_SIZE = 1000;
    static final int MAX_WAIT = 1000;
	
	public MessageBuffer(@Autowired SolrSampleRepository solrSampleRepository) {
		this.solrSampleRepository = solrSampleRepository;
		messageSampleStatusQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);
		latestTime = new AtomicLong(0);
	}
	
	public MessageSampleStatus recieve(SolrSample sample) throws InterruptedException {
		//if there is no maximum time
		//set it to wait in the future
		latestTime.compareAndSet(0, Instant.now().toEpochMilli()+MAX_WAIT);
		
		MessageSampleStatus status = MessageSampleStatus.build(sample);
		
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
		if (remaining <= 0.1*QUEUE_SIZE
				|| (now > latestTimeLong && latestTimeLong != 0)) {
							
			//unset the latest time so that it can be set again by the next message
			latestTime.set(0);
			
			//create a local collection of the messages
			List<MessageSampleStatus> messageSampleStatuses = new ArrayList<>(QUEUE_SIZE);
			//drain the master queue into it
			messageSampleStatusQueue.drainTo(messageSampleStatuses, QUEUE_SIZE);					
			//now we can process the local copy without worrying about new ones being added
			
			//split out the samples into a separate list
			List<SolrSample> samples = new ArrayList<>(messageSampleStatuses.size());
			messageSampleStatuses.stream().forEach(m -> samples.add(m.sample));
	
			//send everything to solr as a single commit
			solrSampleRepository.save(samples);
			//this was a hard commit so they are now written to disk
			
			//if there was a problem, an exception would be thrown
			//since we are still here, no unrecoverable problems occurred
			
			//mark each of the status as completed
			//this will trigger the waiting threads to continue
			messageSampleStatuses.stream().forEach(m -> m.storedInSolr.set(true));
			
		}
	}
	
}
