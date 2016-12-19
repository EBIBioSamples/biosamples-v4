package uk.ac.ebi.biosamples.mongo;

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
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoSubmission;
import uk.ac.ebi.biosamples.mongo.repo.MongoSubmissionRepository;


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
		log.trace("@HandleBeforeCreate triggered");
		MongoSubmission sub = new MongoSubmission(sample);
		subsRepo.save(sub);
	}
	
	@HandleBeforeSave
	public void onBeforeSaveEvent(MongoSample sample) {
		log.trace("@HandleBeforeSave triggered");
		MongoSubmission sub = new MongoSubmission(sample);
		subsRepo.save(sub);		
	}
	
	@HandleAfterCreate
	public void onAfterCreateEvent(MongoSample sample) {
		log.trace("@HandleAfterCreate triggered");
		amqpTemplate.convertAndSend(Messaging.exchangeForIndexing, "", sample);
	}
	
	@HandleAfterSave
	public void onAfterSaveEvent(MongoSample sample) {
		log.trace("@HandleAfterSave triggered");
		amqpTemplate.convertAndSend(Messaging.exchangeForIndexing, "", sample);
	}
}
