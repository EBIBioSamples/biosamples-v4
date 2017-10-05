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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	@Qualifier("accessionJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;
	
	public Optional<String> getDomainForApiKey(String apiKey) throws DataAccessException {
		log.info("getting domain for apikey "+apiKey);

		String stm = "SELECT AAPDOMAIN FROM USERS WHERE APIKEY LIKE ?";
    	List<String> results = jdbcTemplate.query(stm, new SingleStringRowMapper(), apiKey);
    	if (results.size() == 0) {
    		return Optional.empty();
    	} else {
    		return Optional.of(results.get(0));
    	}
	}
	
	public Optional<String> getUsernameForApiKey(String apiKey) throws DataAccessException {
		log.info("getting domain for apikey "+apiKey);

		String stm = "SELECT USERNAME FROM USERS WHERE APIKEY LIKE ?";
    	List<String> results = jdbcTemplate.query(stm, new SingleStringRowMapper(), apiKey);
    	if (results.size() == 0) {
    		return Optional.empty();
    	} else {
    		return Optional.of(results.get(0));
    	}
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
	
    protected static class SingleStringRowMapper implements RowMapper<String>
	{
		public String mapRow(ResultSet rs, int rowNum) throws SQLException {
			return rs.getString(1);
		}
	}

}
