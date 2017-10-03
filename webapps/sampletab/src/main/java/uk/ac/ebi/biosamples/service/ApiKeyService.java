package uk.ac.ebi.biosamples.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	@Qualifier("accessionJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;
	
	public Optional<String> getDomainForApiKey(String apiKey) throws DataAccessException {
		log.info("getting domain for apikey "+apiKey);
		return Optional.ofNullable(jdbcTemplate.queryForObject("SELECT AAPDOMAIN FROM USERS WHERE LIKE APIKEY ?", String.class, apiKey));
	}
}
