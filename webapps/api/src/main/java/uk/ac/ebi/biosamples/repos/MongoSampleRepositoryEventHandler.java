package uk.ac.ebi.biosamples.repos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.models.MongoSample;
import uk.ac.ebi.biosamples.models.MongoSubmission;

@Service
@RepositoryEventHandler
public class MongoSampleRepositoryEventHandler {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private MongoSubmissionRepository subsRepo;
	
	@HandleBeforeCreate
	public void onBeforeCreateEvent(MongoSample sample) {
		log.info("@HandleBeforeCreate triggered");
		MongoSubmission sub = new MongoSubmission(sample);
		subsRepo.save(sub);
	}
	
	@HandleBeforeSave
	public void onBeforeSaveEvent(MongoSample sample) {
		log.info("@HandleBeforeSave triggered");
		MongoSubmission sub = new MongoSubmission(sample);
		subsRepo.save(sub);		
	}
}
