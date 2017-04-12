package uk.ac.ebi.biosamples.neo;

import java.util.Collection;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.biosamples.messages.threaded.MessageBuffer;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class NeoMessageBuffer extends MessageBuffer<NeoSample, NeoSampleRepository> {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	public NeoMessageBuffer(NeoSampleRepository repository) {
		super(repository);
	}

	@Override
	protected void save(NeoSampleRepository repository, Collection<NeoSample> samples) {
		int retryCount = 0;
		while (retryCount < 100) {
//			try {
				//this will have its own transaction
				repository.save(samples);
				return;
//			} catch (TransientException e) {
//				retryCount += 1 ;
//				log.info("Retrying due to transient exception. Attempt number "+retryCount);
//			}
		}
		log.warn("Unable to save within "+retryCount+" retries");
	}

}
