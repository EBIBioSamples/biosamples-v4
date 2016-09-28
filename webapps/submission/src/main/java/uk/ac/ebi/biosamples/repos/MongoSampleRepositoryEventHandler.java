package uk.ac.ebi.biosamples.repos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
	private MongoSampleRepository repo;
	
	@HandleBeforeCreate
	public void onBeforeCreateEvent(MongoSample sample) throws AlreadySubmittedException {
		log.info("@HandleBeforeCreate triggered");
		//check if this is a new accession
		String acc = sample.getAccession();
		Page<MongoSample> page = repo.findByAccession(acc, new PageRequest(1,1));
		if (page.getTotalElements() > 0) {
			//there was at least one existing one
			throw new AlreadySubmittedException();
		}
	}
	
	@HandleBeforeSave
	public void onBeforeSaveEvent(MongoSample sample) {
		log.info("@HandleBeforeSave triggered");
		//turn this into a create by removing the id
		sample.setId(null);
	}
}
