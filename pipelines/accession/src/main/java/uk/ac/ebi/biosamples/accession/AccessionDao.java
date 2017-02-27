package uk.ac.ebi.biosamples.accession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

@Service
public class AccessionDao {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	@Qualifier("accessionJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;

	public void doAssayAccessionCallback(RowCallbackHandler rch) {
		String sql = "SELECT * FROM SAMPLE_ASSAY";
		jdbcTemplate.query(sql, rch);
	}

	public void doReferenceAccessionCallback(RowCallbackHandler rch) {
		String sql = "SELECT * FROM SAMPLE_REFERENCE";
		jdbcTemplate.query(sql, rch);
	}

	public void doGroupAccessionCallback(RowCallbackHandler rch) {
		String sql = "SELECT * FROM SAMPLE_GROUP";
		jdbcTemplate.query(sql, rch);
	}
}
