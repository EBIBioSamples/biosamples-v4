/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.ena;

import java.time.LocalDate;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class EraProDao {
  @Autowired
  @Qualifier("eraJdbcTemplate")
  protected JdbcTemplate jdbcTemplate;

  private final Logger log = LoggerFactory.getLogger(getClass());

  private static final String STATUS_CLAUSE = "STATUS_ID IN (3, 4, 5, 6, 7, 8)";

  /*private static final String SQL_WWWDEV_MAPPING =
        "SELECT BIOSAMPLE_ID FROM SAMPLE WHERE SUBMISSION_ACCOUNT_ID = 'Webin-161' AND BIOSAMPLE_AUTHORITY= 'N' AND ((LAST_UPDATED BETWEEN TO_DATE('2022-01-01', 'YYYY-MM-DD') AND TO_DATE('2022-07-15', 'YYYY-MM-DD')) OR (FIRST_PUBLIC BETWEEN TO_DATE('2022-01-01', 'YYYY-MM-DD') AND TO_DATE('2022-07-15', 'YYYY-MM-DD'))) ORDER BY BIOSAMPLE_ID DESC";
  */
  public void doSampleCallback(
      final LocalDate minDate, final LocalDate maxDate, final RowCallbackHandler rch) {
    final String query =
        "SELECT UNIQUE(BIOSAMPLE_ID), STATUS_ID, EGA_ID, LAST_UPDATED FROM SAMPLE WHERE BIOSAMPLE_ID LIKE 'SAME%' AND SAMPLE_ID LIKE 'ERS%' AND BIOSAMPLE_AUTHORITY= 'N' "
            + "AND "
            + STATUS_CLAUSE
            + " AND ((LAST_UPDATED BETWEEN ? AND ?) OR (FIRST_PUBLIC BETWEEN ? AND ?)) ORDER BY LAST_UPDATED ASC";

    final Date minDateOld = java.sql.Date.valueOf(minDate);
    final Date maxDateOld = java.sql.Date.valueOf(maxDate);

    jdbcTemplate.query(query, rch, minDateOld, maxDateOld, minDateOld, maxDateOld);
  }

  /*public List<String> doWWWDEVMapping() {
    return jdbcTemplate.queryForList(SQL_WWWDEV_MAPPING, String.class);
  }*/

  public void getSingleSample(final String bioSampleId, final RowCallbackHandler rch) {
    final String query =
        "SELECT UNIQUE(BIOSAMPLE_ID), STATUS_ID FROM SAMPLE WHERE BIOSAMPLE_ID LIKE 'SAME%' AND SAMPLE_ID LIKE 'ERS%' AND EGA_ID IS NULL AND BIOSAMPLE_AUTHORITY= 'N' "
            + "AND "
            + STATUS_CLAUSE
            + " AND BIOSAMPLE_ID=? ORDER BY BIOSAMPLE_ID ASC";

    jdbcTemplate.query(query, rch, bioSampleId);
  }

  public String getBioSampleAccessionByEnaAccession(final String enaId) {
    final String query = "SELECT BIOSAMPLE_ID FROM SAMPLE WHERE SAMPLE_ID = ?";

    return jdbcTemplate.queryForObject(query, String.class, new Object[] {enaId});
  }

  public void getNcbiCallback(
      final LocalDate minDate, final LocalDate maxDate, final RowCallbackHandler rch) {
    String query =
        "SELECT UNIQUE(BIOSAMPLE_ID), STATUS_ID, LAST_UPDATED FROM SAMPLE WHERE (BIOSAMPLE_ID LIKE 'SAMN%' OR BIOSAMPLE_ID LIKE 'SAMD%' ) AND BIOSAMPLE_AUTHORITY= 'N' "
            + "AND "
            + STATUS_CLAUSE
            + " AND ((LAST_UPDATED BETWEEN ? AND ?) OR (FIRST_PUBLIC BETWEEN ? AND ?)) ORDER BY LAST_UPDATED ASC";

    final Date minDateOld = java.sql.Date.valueOf(minDate);
    final Date maxDateOld = java.sql.Date.valueOf(maxDate);

    jdbcTemplate.query(query, rch, minDateOld, maxDateOld, minDateOld, maxDateOld);
  }

  public SampleDBBean getSampleDetailsByBioSampleId(final String biosampleAccession) {
    try {
      String sql =
          "SELECT SAMPLE_XML, TAX_ID, "
              + "to_char(LAST_UPDATED, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') AS LAST_UPDATED, "
              + "to_char(FIRST_PUBLIC, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') AS FIRST_PUBLIC,  "
              + "to_char(FIRST_CREATED, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') AS FIRST_CREATED, "
              + "STATUS_ID, "
              + "SUBMISSION_ACCOUNT_ID "
              + "FROM SAMPLE "
              + "WHERE BIOSAMPLE_ID = ? fetch first row only ";
      final SampleDBBean sampleData =
          jdbcTemplate.queryForObject(sql, sampleRowMapper, biosampleAccession);

      return sampleData;
    } catch (final IncorrectResultSizeDataAccessException e) {
      log.error(
          "Result set size expected is 1 and got more/ less that that, skipping "
              + biosampleAccession);
    }

    return null;
  }

  public SampleDBBean getSampleMetaInfoByBioSampleId(final String sampleId) {
    try {
      final String sql =
          "SELECT STATUS_ID, SAMPLE_ID, "
              + "BIOSAMPLE_ID, "
              + "BIOSAMPLE_AUTHORITY "
              + "FROM SAMPLE "
              + "WHERE BIOSAMPLE_ID = ? AND BIOSAMPLE_AUTHORITY= 'N' "
              + "fetch first row only ";
      final SampleDBBean sampleData = jdbcTemplate.queryForObject(sql, sampleRowMapper2, sampleId);

      return sampleData;
    } catch (final IncorrectResultSizeDataAccessException e) {
      log.error("Result set size expected is 1 and got more/ less that that, skipping");
    }

    return null;
  }

  public SampleDBBean getSampleDetailsByEnaSampleId(final String sampleId) {
    try {
      final String sql =
          "SELECT STATUS_ID, SAMPLE_ID, "
              + "BIOSAMPLE_ID, "
              + "BIOSAMPLE_AUTHORITY "
              + "FROM SAMPLE "
              + "WHERE SAMPLE_ID = ? AND BIOSAMPLE_AUTHORITY= 'N' "
              + "fetch first row only ";
      final SampleDBBean sampleData = jdbcTemplate.queryForObject(sql, sampleRowMapper2, sampleId);

      return sampleData;
    } catch (final IncorrectResultSizeDataAccessException e) {
      log.error("Result set size expected is 1 and got more/ less that that, skipping");
    }

    return null;
  }

  public void getEnaDatabaseSample(final String enaAccession, final RowCallbackHandler rch) {
    final String query =
        "select BIOSAMPLE_ID,\n"
            + "       FIXED_TAX_ID, "
            + "       FIXED_SCIENTIFIC_NAME, "
            + "       FIXED_COMMON_NAME, "
            + "       FIXED, "
            + "       TAX_ID, "
            + "       SCIENTIFIC_NAME, "
            + "       to_char(first_public, 'yyyy-mm-dd')                                                as first_public,\n"
            + "       to_char(last_updated, 'yyyy-mm-dd')                                                as last_updated,\n"
            + "       (select nvl(cv_broker_name.description, T1.broker_name)\n"
            + "        from XMLTable('/SAMPLE_SET[ 1 ]/SAMPLE/@broker_name' passing sample.sample_xml\n"
            + "                      columns broker_name varchar(1000) path '.') T1\n"
            + "               left join cv_broker_name on (cv_broker_name.broker_name = T1.broker_name)) as broker_name,\n"
            + "       (select nvl(cv_center_name.description, T2.center_name)\n"
            + "        from XMLTable('/SAMPLE_SET[ 1 ]/SAMPLE/@center_name' passing sample.sample_xml\n"
            + "                      columns center_name varchar(1000) path '.') T2\n"
            + "               left join cv_center_name on (cv_center_name.center_name = T2.center_name)) as center_name\n"
            + "from SAMPLE\n"
            + "where BIOSAMPLE_ID = ?";

    jdbcTemplate.query(query, rch, enaAccession);
  }

  RowMapper<SampleDBBean> sampleRowMapper =
      (rs, rowNum) -> {
        final SampleDBBean sampleBean = new SampleDBBean();

        sampleBean.setSampleXml(rs.getString("SAMPLE_XML"));
        sampleBean.setFirstPublic(rs.getString("FIRST_PUBLIC"));
        sampleBean.setLastUpdate(rs.getString("LAST_UPDATED"));
        sampleBean.setFirstCreated(rs.getString("FIRST_CREATED"));
        sampleBean.setStatus(rs.getInt("STATUS_ID"));
        sampleBean.setSubmissionAccountId(rs.getString("SUBMISSION_ACCOUNT_ID"));
        sampleBean.setTaxId(rs.getLong("TAX_ID"));

        return sampleBean;
      };

  RowMapper<SampleDBBean> sampleRowMapper2 =
      (rs, rowNum) -> {
        final SampleDBBean sampleBean = new SampleDBBean();

        sampleBean.setStatus(rs.getInt("STATUS_ID"));
        sampleBean.setSampleId(rs.getString("SAMPLE_ID"));
        sampleBean.setBiosampleId(rs.getString("BIOSAMPLE_ID"));
        sampleBean.setBiosampleAuthority(rs.getString("BIOSAMPLE_AUTHORITY"));

        return sampleBean;
      };
}
