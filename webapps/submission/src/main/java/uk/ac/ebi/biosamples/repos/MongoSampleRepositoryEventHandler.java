package uk.ac.ebi.biosamples.repos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleAfterLinkSave;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.models.MongoSample;


@Service
@RepositoryEventHandler
public class MongoSampleRepositoryEventHandler {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	//private MongoSampleCreateReadRepository repo;
	private MongoSampleRepository repo;
	
	@HandleBeforeCreate
	public void onBeforeCreateEvent(MongoSample sample) {
		log.info("HandleBeforeCreate triggered");
		//check if this is a new accession
		String acc = sample.getAccession();
		//repo.findByAccession(acc);
	}
	
	@HandleBeforeSave
	public void onBeforeSaveEvent(MongoSample sample) {
		log.info("@HandleBeforeSave triggered");
		//turn this into a create by removing the id
		sample.setId(null);
	}
}
