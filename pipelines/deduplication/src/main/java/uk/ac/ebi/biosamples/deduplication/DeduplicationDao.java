package uk.ac.ebi.biosamples.deduplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Lazy
@Service
public class DeduplicationDao {
    @Autowired
    @Qualifier("eraJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;
    private Logger log = LoggerFactory.getLogger(getClass());

    public List<RowMapping> getAllSamples() {
        final String query = "SELECT SAMPLE_ID, BIOSAMPLE_ID FROM SAMPLE WHERE SAMPLE_ID LIKE 'SRS%'";

        return jdbcTemplate.query(query, mapper);
    }

    static class RowMapping {
        String enaId;
        String bioSampleId;

        public String getEnaId() {
            return enaId;
        }

        public void setEnaId(String enaId) {
            this.enaId = enaId;
        }

        public String getBioSampleId() {
            return bioSampleId;
        }

        public void setBioSampleId(String bioSampleId) {
            this.bioSampleId = bioSampleId;
        }
    }

    final RowMapper<RowMapping> mapper = (rs, rowNum) -> {
        RowMapping rowMapping = new RowMapping();

        rowMapping.setBioSampleId(rs.getString("BIOSAMPLE_ID"));
        rowMapping.setEnaId(rs.getString("SAMPLE_ID"));

        return rowMapping;
    };
}
