package uk.ac.ebi.biosamples.ena;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

@Service
public class EraProDao {

    @Autowired
    @Qualifier("eraJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;

    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Return a set of BioSamples accessions that have been updated or made public within
     * the specified date range
     *
     * @param minDate
     * @param maxDate
     * @return
     */
    public SortedSet<String> getSamples(LocalDate minDate, LocalDate maxDate) {
        /*
select * from cv_status;
1       draft   The entry is draft.
2       private The entry is private.
3       cancelled       The entry has been cancelled.
4       public  The entry is public.
5       suppressed      The entry has been suppressed.
6       killed  The entry has been killed.
7       temporary_suppressed    the entry has been temporarily suppressed.
8       temporary_killed        the entry has been temporarily killed.
         */
        //once it has been public, it can only be suppressed and killed and can't go back to public again

        String query = "SELECT BIOSAMPLE_ID FROM SAMPLE WHERE BIOSAMPLE_ID LIKE 'SAME%' AND EGA_ID IS NULL AND BIOSAMPLE_AUTHORITY= 'N' "
                + "AND STATUS_ID = 4 AND ((LAST_UPDATED BETWEEN ? AND ?) OR (FIRST_PUBLIC BETWEEN ? AND ?)) ORDER BY BIOSAMPLE_ID ASC";

        SortedSet<String> samples = new TreeSet<>();
        Date minDateOld = java.sql.Date.valueOf(minDate);
        Date maxDateOld = java.sql.Date.valueOf(maxDate);
        List<String> result = jdbcTemplate.queryForList(query, String.class, minDateOld, maxDateOld, minDateOld, maxDateOld);
        samples.addAll(result);
        return samples;
    }


    public void doSampleCallback(LocalDate minDate, LocalDate maxDate, RowCallbackHandler rch) {
        String query = "SELECT UNIQUE(BIOSAMPLE_ID) FROM SAMPLE WHERE BIOSAMPLE_ID LIKE 'SAME%' AND SAMPLE_ID LIKE 'ERS%' AND EGA_ID IS NULL AND BIOSAMPLE_AUTHORITY= 'N' "
                + "AND STATUS_ID = 4 AND ((LAST_UPDATED BETWEEN ? AND ?) OR (FIRST_PUBLIC BETWEEN ? AND ?)) ORDER BY BIOSAMPLE_ID ASC";

        Date minDateOld = java.sql.Date.valueOf(minDate);
        Date maxDateOld = java.sql.Date.valueOf(maxDate);
        jdbcTemplate.query(query, rch, minDateOld, maxDateOld, minDateOld, maxDateOld);
    }

    public void getSingleSample(String bioSampleId, RowCallbackHandler rch) {
        String query = "SELECT UNIQUE(BIOSAMPLE_ID) FROM SAMPLE WHERE BIOSAMPLE_ID LIKE 'SAME%' AND SAMPLE_ID LIKE 'ERS%' AND EGA_ID IS NULL AND BIOSAMPLE_AUTHORITY= 'N' "
                + "AND STATUS_ID = 4 AND BIOSAMPLE_ID=? ORDER BY BIOSAMPLE_ID ASC";
        jdbcTemplate.query(query, rch, bioSampleId);
    }

    public void getNcbiCallback(LocalDate minDate, LocalDate maxDate, RowCallbackHandler rch) {

        String query = "SELECT UNIQUE(BIOSAMPLE_ID) FROM SAMPLE WHERE (BIOSAMPLE_ID LIKE 'SAMN%' OR BIOSAMPLE_ID LIKE 'SAMD%' ) "
                + "AND STATUS_ID = 4 AND ((LAST_UPDATED BETWEEN ? AND ?) OR (FIRST_PUBLIC BETWEEN ? AND ?)) ORDER BY BIOSAMPLE_ID ASC";

        Date minDateOld = java.sql.Date.valueOf(minDate);
        Date maxDateOld = java.sql.Date.valueOf(maxDate);
        jdbcTemplate.query(query, rch, minDateOld, maxDateOld, minDateOld, maxDateOld);
    }

    public List<String> getPrivateSamples() {
        log.trace("Getting private sample ids");

        String query = "SELECT UNIQUE(BIOSAMPLE_ID) FROM SAMPLE WHERE STATUS_ID > 4 AND BIOSAMPLE_ID LIKE 'SAME%' "
                + "AND EGA_ID IS NULL AND BIOSAMPLE_AUTHORITY= 'N' ORDER BY BIOSAMPLE_ID ASC";

        List<String> sampleIds = jdbcTemplate.queryForList(query, String.class);

        log.info("Got " + sampleIds.size() + " private sample ids");

        return sampleIds;
    }

    public boolean getBioSamplesAuthority(String biosampleAccession) {
        String query = "SELECT BIOSAMPLE_AUTHORITY FROM SAMPLE WHERE BIOSAMPLE_ID = ? ";
        String result = jdbcTemplate.queryForObject(query, new RowMapper<String>() {

            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString(1);
            }
        }, biosampleAccession);
        if (result.equals("Y")) {
            return true;
        } else if (result.equals("N")) {
            return false;
        } else {
            throw new IllegalArgumentException("Unrecongized BIOSAMPLE_AUTHORITY " + result);
        }
    }

    public Instant getUpdateDateTime(String biosampleAccession) {
        String sql = "SELECT to_char(LAST_UPDATED, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') FROM SAMPLE WHERE BIOSAMPLE_ID = ? AND BIOSAMPLE_AUTHORITY='N' AND SAMPLE_ID LIKE 'ERS%'";
        String dateString = jdbcTemplate.queryForObject(sql, String.class, biosampleAccession);
        log.trace("Update date of \"+biosampleAccession+\"is " + dateString);
        return Instant.parse(dateString);
    }

    public String getCentreName(String biosampleAccession) {
        String sql = "SELECT CENTER_NAME FROM SAMPLE WHERE BIOSAMPLE_ID = ? AND BIOSAMPLE_AUTHORITY='N' AND SAMPLE_ID LIKE 'ERS%'";
        String centerName = jdbcTemplate.queryForObject(sql, String.class, biosampleAccession);
        log.trace("Center name of " + biosampleAccession + " is " + centerName);
        return centerName;
    }

    public String getChecklist(String biosampleAccession) {
        String sql = "SELECT CHECKLIST_ID FROM SAMPLE WHERE BIOSAMPLE_ID = ? AND BIOSAMPLE_AUTHORITY='N' AND SAMPLE_ID LIKE 'ERS%'";
        String checklist = jdbcTemplate.queryForObject(sql, String.class, biosampleAccession);
        log.trace("Checklist of " + biosampleAccession + " is " + checklist);
        return checklist;
    }

    public String getStatus(String biosampleAccession) {
        String sql = "SELECT STATUS_ID FROM SAMPLE WHERE BIOSAMPLE_ID = ? AND BIOSAMPLE_AUTHORITY='N' AND SAMPLE_ID LIKE 'ERS%'";
        Integer statusId = jdbcTemplate.queryForObject(sql, Integer.class, biosampleAccession);
        log.trace("Status of " + biosampleAccession + " is " + statusId);
        if (1 == statusId) {
            return "draft";
        } else if (2 == statusId) {
            return "private";
        } else if (3 == statusId) {
            return "cancelled";
        } else if (4 == statusId) {
            //use "live" for consistency with NCBI
            return "live";
            //return "public";
        } else if (5 == statusId) {
            return "suppressed";
        } else if (6 == statusId) {
            return "killed";
        } else if (7 == statusId) {
            return "temporary_suppressed";
        } else if (8 == statusId) {
            return "temporary_killed";
        }
        throw new RuntimeException("Unrecognised statusid " + statusId);
    }

    public Instant getReleaseDateTime(String biosampleAccession) {
        String sql = "SELECT to_char(FIRST_PUBLIC, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') FROM SAMPLE WHERE BIOSAMPLE_ID = ? AND BIOSAMPLE_AUTHORITY='N' AND SAMPLE_ID LIKE 'ERS%'";
        String dateString = jdbcTemplate.queryForObject(sql, String.class, biosampleAccession);
        log.trace("Release date of \"+biosampleAccession+\"is " + dateString);
        return Instant.parse(dateString);
    }

    public String getSampleXml(String biosampleAccession) throws SQLException {
        String sql = "SELECT SAMPLE_XML FROM SAMPLE WHERE BIOSAMPLE_ID = ? AND BIOSAMPLE_AUTHORITY='N' AND SAMPLE_ID LIKE 'ERS%'";
        String result = jdbcTemplate.queryForObject(sql, String.class, biosampleAccession);
        return result;

    }
}
