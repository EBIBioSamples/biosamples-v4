package uk.ac.ebi.biosamples.ena;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

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

    private static final String STATUS_CLAUSE = "STATUS_ID IN (4, 5, 6, 7, 8)";

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
                + "AND " + STATUS_CLAUSE + " AND ((LAST_UPDATED BETWEEN ? AND ?) OR (FIRST_PUBLIC BETWEEN ? AND ?)) ORDER BY BIOSAMPLE_ID ASC";

        SortedSet<String> samples = new TreeSet<>();
        Date minDateOld = java.sql.Date.valueOf(minDate);
        Date maxDateOld = java.sql.Date.valueOf(maxDate);
        List<String> result = jdbcTemplate.queryForList(query, String.class, minDateOld, maxDateOld, minDateOld, maxDateOld);
        samples.addAll(result);
        return samples;
    }


    public void doSampleCallback(LocalDate minDate, LocalDate maxDate, RowCallbackHandler rch) {
        String query = "SELECT UNIQUE(BIOSAMPLE_ID), STATUS_ID FROM SAMPLE WHERE BIOSAMPLE_ID LIKE 'SAME%' AND SAMPLE_ID LIKE 'ERS%' AND EGA_ID IS NULL AND BIOSAMPLE_AUTHORITY= 'N' "
                + "AND " + STATUS_CLAUSE + " AND ((LAST_UPDATED BETWEEN ? AND ?) OR (FIRST_PUBLIC BETWEEN ? AND ?)) ORDER BY BIOSAMPLE_ID ASC";

        Date minDateOld = java.sql.Date.valueOf(minDate);
        Date maxDateOld = java.sql.Date.valueOf(maxDate);
        jdbcTemplate.query(query, rch, minDateOld, maxDateOld, minDateOld, maxDateOld);
    }

    public void getSingleSample(String bioSampleId, RowCallbackHandler rch) {
        String query = "SELECT UNIQUE(BIOSAMPLE_ID), STATUS_ID FROM SAMPLE WHERE BIOSAMPLE_ID LIKE 'SAME%' AND SAMPLE_ID LIKE 'ERS%' AND EGA_ID IS NULL AND BIOSAMPLE_AUTHORITY= 'N' "
                + "AND " + STATUS_CLAUSE + " AND BIOSAMPLE_ID=? ORDER BY BIOSAMPLE_ID ASC";
        jdbcTemplate.query(query, rch, bioSampleId);
    }

    public void getNcbiCallback(LocalDate minDate, LocalDate maxDate, RowCallbackHandler rch) {

        String query = "SELECT UNIQUE(BIOSAMPLE_ID), STATUS_ID FROM SAMPLE WHERE (BIOSAMPLE_ID LIKE 'SAMN%' OR BIOSAMPLE_ID LIKE 'SAMD%' ) "
                + "AND " + STATUS_CLAUSE + " AND ((LAST_UPDATED BETWEEN ? AND ?) OR (FIRST_PUBLIC BETWEEN ? AND ?)) ORDER BY BIOSAMPLE_ID ASC";

        Date minDateOld = java.sql.Date.valueOf(minDate);
        Date maxDateOld = java.sql.Date.valueOf(maxDate);
        jdbcTemplate.query(query, rch, minDateOld, maxDateOld, minDateOld, maxDateOld);
    }

    public Instant getUpdateDateTime(String biosampleAccession) {
        String sql = "SELECT to_char(LAST_UPDATED, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') FROM SAMPLE WHERE BIOSAMPLE_ID = ? AND BIOSAMPLE_AUTHORITY='N' AND SAMPLE_ID LIKE 'ERS%'";
        String dateString = jdbcTemplate.queryForObject(sql, String.class, biosampleAccession);
        log.trace("Update date of \"+biosampleAccession+\"is " + dateString);
        return Instant.parse(dateString);
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
            return "public";
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
        if (dateString == null) {
            return null;
        }
        log.trace("Release date of \"+biosampleAccession+\"is " + dateString);
        return Instant.parse(dateString);
    }

    public String getSampleXml(String biosampleAccession) throws SQLException {
        String sql = "SELECT SAMPLE_XML FROM SAMPLE WHERE BIOSAMPLE_ID = ? AND BIOSAMPLE_AUTHORITY='N' AND SAMPLE_ID LIKE 'ERS%'";
        String result = jdbcTemplate.queryForObject(sql, String.class, biosampleAccession);
        return result;
    }

    public void getEnaDatabaseSample(String enaAccession, RowCallbackHandler rch) {
        String query = "select BIOSAMPLE_ID,\n" +
                "       FIXED_TAX_ID, " +
                "       FIXED_SCIENTIFIC_NAME, " +
                "       FIXED_COMMON_NAME, " +
                "       FIXED, " +
                "       TAX_ID, " +
                "       SCIENTIFIC_NAME, " +
                "       to_char(first_public, 'yyyy-mm-dd')                                                as first_public,\n" +
                "       to_char(last_updated, 'yyyy-mm-dd')                                                as last_updated,\n" +
                "       (select nvl(cv_broker_name.description, T1.broker_name)\n" +
                "        from XMLTable('/SAMPLE_SET[ 1 ]/SAMPLE/@broker_name' passing sample.sample_xml\n" +
                "                      columns broker_name varchar(1000) path '.') T1\n" +
                "               left join cv_broker_name on (cv_broker_name.broker_name = T1.broker_name)) as broker_name,\n" +
                "       (select nvl(cv_center_name.description, T2.center_name)\n" +
                "        from XMLTable('/SAMPLE_SET[ 1 ]/SAMPLE/@center_name' passing sample.sample_xml\n" +
                "                      columns center_name varchar(1000) path '.') T2\n" +
                "               left join cv_center_name on (cv_center_name.center_name = T2.center_name)) as center_name\n" +
                "from SAMPLE\n" +
                "where BIOSAMPLE_ID = ?";
        jdbcTemplate.query(query, rch, enaAccession);
    }
}
