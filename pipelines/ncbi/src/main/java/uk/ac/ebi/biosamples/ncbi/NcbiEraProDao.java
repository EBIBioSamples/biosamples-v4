package uk.ac.ebi.biosamples.ncbi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Lazy
@Service
public class NcbiEraProDao {
    @Autowired
    @Qualifier("eraJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;

    public String getSraAccession(final String sampleAccession) {
        String sql = "SELECT SAMPLE_ID FROM SAMPLE WHERE BIOSAMPLE_ID = ? ";

        List<String> resultList = jdbcTemplate.queryForList(
                sql, new Object[]{sampleAccession}, String.class);

        return resultList.get(0);
    }
}
