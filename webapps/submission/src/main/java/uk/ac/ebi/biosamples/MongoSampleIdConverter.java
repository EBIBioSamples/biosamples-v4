package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.models.MongoSample;
import uk.ac.ebi.biosamples.repos.MongoSampleRepository;

import java.io.Serializable;


@Component
public class MongoSampleIdConverter implements BackendIdConverter {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private MongoSampleRepository repo;

	@Override
	public Serializable fromRequestId(String id, Class<?> entityType) {
		log.info("fromRequestId "+id);
		return id;
	}
	
	@Override
	public String toRequestId(Serializable id, Class<?> entityType) {

		log.info("toRequestId "+id);
		
		return id.toString();
	}
	
	@Override
	public boolean supports(Class<?> delimiter) {
		return MongoSample.class.isAssignableFrom(delimiter);
	}
}
