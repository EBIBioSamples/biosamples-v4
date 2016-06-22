package uk.ac.ebi.biosamples.repos;

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
	
	
	@Autowired
	private MongoSampleCreateReadRepository repo;
	
	@HandleBeforeCreate
	public void onBeforeCreateEvent(MongoSample sample) {
		//check if this is a new accession, if not then throw a 
		sample.getAccession();
	}
	@HandleBeforeSave
	public void onBeforeSaveEvent(MongoSample sample) {
		//turn this into a 	
	}
}
