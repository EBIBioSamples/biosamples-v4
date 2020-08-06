package uk.ac.ebi.biosamples.ebeye.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class EbeyeBioSamplesDataDumpGeneratorDao {
    @Autowired
    @Qualifier("eraJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;

    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Returns Sample status
     *
     * @return
     */
    public List<Integer> doGetSampleStatus(String accession) {
        String query = "SELECT STATUS_ID FROM SAMPLE WHERE BIOSAMPLE_ID = :accession";

        List<Integer> result = jdbcTemplate.queryForList(query, Integer.class, accession);
        return result;
    }
}
