package uk.ac.ebi.biosamples.mongo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoAction;
import org.springframework.data.mongodb.core.WriteConcernResolver;
import org.springframework.stereotype.Component;

import com.mongodb.WriteConcern;

import uk.ac.ebi.biosamples.mongo.MongoProperties;
import uk.ac.ebi.biosamples.mongo.model.MongoSubmission;

//TODO wire this into config
@Component
public class CustomWriteConcernResolver implements WriteConcernResolver {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private MongoProperties mongoProperties;
	
	@Override
	public WriteConcern resolve(MongoAction action) {
		log.trace("Resolving mongoAction "+action);
		if (MongoSubmission.class.isAssignableFrom(action.getEntityType())) {
			if (mongoProperties.getSubmissionWriteConcern().matches("[0-9]+")) {
				return new WriteConcern(Integer.parseInt(mongoProperties.getSubmissionWriteConcern()));
			} else {
				return new WriteConcern(mongoProperties.getSubmissionWriteConcern());
			}
		}
		
		return action.getDefaultWriteConcern();
	}

}
