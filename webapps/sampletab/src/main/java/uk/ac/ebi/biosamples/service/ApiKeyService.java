package uk.ac.ebi.biosamples.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.mongo.model.MongoSampleTabApiKey;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleTabApiKeyRepository;

@Service
public class ApiKeyService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	public final static String BIOSAMPLES="BioSamples";

	private final MongoSampleTabApiKeyRepository mongoSampleTabApiKeyRepository;
	
	public ApiKeyService(MongoSampleTabApiKeyRepository mongoSampleTabApiKeyRepository) {
		this.mongoSampleTabApiKeyRepository = mongoSampleTabApiKeyRepository;
	}
	
	public Optional<String> getDomainForApiKey(String apiKey) throws DataAccessException {
		log.info("getting domain for apikey "+apiKey);
		MongoSampleTabApiKey mongoSampleTabApiKey = mongoSampleTabApiKeyRepository.findOne(apiKey);
		return Optional.ofNullable(mongoSampleTabApiKey.getAapDomain());
	}
	
	public Optional<String> getUsernameForApiKey(String apiKey) throws DataAccessException {
		log.info("getting domain for apikey "+apiKey);
		MongoSampleTabApiKey mongoSampleTabApiKey = mongoSampleTabApiKeyRepository.findOne(apiKey);
		return Optional.ofNullable(mongoSampleTabApiKey.getUserName());
	}
	

    public boolean canKeyOwnerEditSource(String keyOwner, String source) {
        if (keyOwner == null || keyOwner.trim().length() == 0) {
            throw new IllegalArgumentException("keyOwner must a sensible string");
        }
        if (source == null || source.trim().length() == 0) {
            throw new IllegalArgumentException("source must be a sensible string");
        }
        
        if ("BioSamples".toLowerCase().equals(keyOwner.toLowerCase())) {
            //BioSamples key can edit anything
            return true;
        } else if (source.toLowerCase().equals(keyOwner.toLowerCase())) {
            //source key can edit their own samples
            return true;
        } else {
            //deny everyone else
        	log.info("Keyowner "+keyOwner+" attempted to access "+source);
            return false;
        }
    }
	

}
