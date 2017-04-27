package uk.ac.ebi.biosamples.neo;

import java.util.Collection;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.messages.threaded.MessageBuffer;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;

@Component
public class NeoMessageBuffer extends MessageBuffer<NeoSample, NeoSampleRepository> {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Random random = new Random();
	
	public NeoMessageBuffer(NeoSampleRepository repository) {
		super(repository);
	}

	@Override
	protected void save(NeoSampleRepository repository, Collection<NeoSample> samples) {
		int retryCount = 0;
		while (retryCount < 100) {
			try {
				//this will have its own transaction
				repository.save(samples);
				return;
			} catch (ConcurrencyFailureException e) {
				retryCount += 1 ;
				log.info("Retrying due to transient exception. Attempt number "+retryCount);
				try {
					Thread.sleep(random.nextInt(400)+100);
				} catch (InterruptedException e1) {
					//do nothing
				}
			}
		}
		log.warn("Unable to save within "+retryCount+" retries");
	}

}
