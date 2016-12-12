package uk.ac.ebi.biosamples.repos.mongo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.models.MongoSample;
import uk.ac.ebi.biosamples.models.MongoSubmission;


@Service
@RepositoryEventHandler
public class MongoSampleRepositoryCustomEventHandler {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private MongoSubmissionRepository subsRepo;
	
	@Autowired
	private AmqpTemplate amqpTemplate;
	
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
	
	@HandleAfterCreate
	public void onAfterCreateEvent(MongoSample sample) {
		log.info("@HandleAfterCreate triggered");
		amqpTemplate.convertAndSend(Messaging.exchangeForIndexing, "", sample);
	}
	
	@HandleAfterSave
	public void onAfterSaveEvent(MongoSample sample) {
		log.info("@HandleAfterSave triggered");
		amqpTemplate.convertAndSend(Messaging.exchangeForIndexing, "", sample);
	}
}
