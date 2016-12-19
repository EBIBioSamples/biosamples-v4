package uk.ac.ebi.biosamples.mongo;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;

@Component
public class SampleBackendIdConverter implements BackendIdConverter {
	
	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private MongoSampleRepository repo;
	
	@Override
	public boolean supports(Class<?> delimiter) {
		return MongoSample.class.isAssignableFrom(delimiter);
	}

	@Override
	public Serializable fromRequestId(String accession, Class<?> clazz) {
		if (accession == null) {
			//log.warn("accession null");
			return null;
		}
		if (!MongoSample.class.isAssignableFrom(clazz)) {
			throw new IllegalArgumentException("Not valid class "+clazz.getCanonicalName());
		}
		log.trace("fromRequestId "+accession);
		return repo.findOneByAccession(accession).id;
	}

	@Override
	public String toRequestId(Serializable id, Class<?> clazz) {
		if (!MongoSample.class.isAssignableFrom(clazz)) {
			throw new IllegalArgumentException("Not valid class "+clazz.getCanonicalName());
		}
		if (id == null) {
			log.warn("toRequestId null");
			return null;		
		}
		log.trace("toRequestId "+id.toString());
		return repo.findOne(id.toString()).accession;
	}

}
