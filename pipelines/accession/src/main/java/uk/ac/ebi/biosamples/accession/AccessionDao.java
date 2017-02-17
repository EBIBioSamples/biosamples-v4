package uk.ac.ebi.biosamples.accession;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AccessionDao {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	@Qualifier("accessionJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;

	
	public List<Map<String, Object>> getAccession() {
		String sql = "SELECT * FROM SAMPLE_ASSAY";
		List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
		return result;
	}
}
