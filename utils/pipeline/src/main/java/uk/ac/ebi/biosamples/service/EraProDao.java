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
package uk.ac.ebi.biosamples.service;

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
  private static final String STATUS_CLAUSE_SUPPRESSED = "STATUS_ID IN (5, 7)";
  private static final String STATUS_CLAUSE_KILLED = "STATUS_ID IN (6, 8)";

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

  /**
   * Returns SUPPRESSED ENA samples
   *
   * @param rch {@link RowCallbackHandler}
   */
  public void doGetSuppressedEnaSamples(final RowCallbackHandler rch) {
    final String query =
        "SELECT UNIQUE(BIOSAMPLE_ID), STATUS_ID FROM SAMPLE WHERE BIOSAMPLE_ID LIKE 'SAME%' AND SAMPLE_ID LIKE 'ERS%' AND EGA_ID IS NULL AND BIOSAMPLE_AUTHORITY= 'N' AND "
            + STATUS_CLAUSE_SUPPRESSED;

    jdbcTemplate.query(query, rch);
  }

  /**
   * Returns KILLED ENA samples
   *
   * @param rch {@link RowCallbackHandler}
   */
  public void doGetKilledEnaSamples(final RowCallbackHandler rch) {
    final String query =
        "SELECT UNIQUE(BIOSAMPLE_ID), STATUS_ID FROM SAMPLE WHERE BIOSAMPLE_ID LIKE 'SAME%' AND SAMPLE_ID LIKE 'ERS%' AND EGA_ID IS NULL AND BIOSAMPLE_AUTHORITY= 'N' AND "
            + STATUS_CLAUSE_KILLED;

    jdbcTemplate.query(query, rch);
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

  public void getNcbiCallback(
      final LocalDate minDate, final LocalDate maxDate, final RowCallbackHandler rch) {
    final String query =
        "SELECT UNIQUE(BIOSAMPLE_ID), STATUS_ID, LAST_UPDATED FROM SAMPLE WHERE (BIOSAMPLE_ID LIKE 'SAMN%' OR BIOSAMPLE_ID LIKE 'SAMD%' ) AND BIOSAMPLE_AUTHORITY= 'N' "
            + "AND "
            + STATUS_CLAUSE
            + " AND ((LAST_UPDATED BETWEEN ? AND ?) OR (FIRST_PUBLIC BETWEEN ? AND ?)) ORDER BY LAST_UPDATED ASC";

    final Date minDateOld = java.sql.Date.valueOf(minDate);
    final Date maxDateOld = java.sql.Date.valueOf(maxDate);

    jdbcTemplate.query(query, rch, minDateOld, maxDateOld, minDateOld, maxDateOld);
  }

  public EraproSample getSampleDetailsByBioSampleId(final String bioSampleId) {
    try {
      final String sql =
          "SELECT SAMPLE_XML, TAX_ID, "
              + "to_char(LAST_UPDATED, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') AS LAST_UPDATED, "
              + "to_char(FIRST_PUBLIC, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') AS FIRST_PUBLIC,  "
              + "to_char(FIRST_CREATED, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') AS FIRST_CREATED, "
              + "STATUS_ID, "
              + "SUBMISSION_ACCOUNT_ID, "
              + "BIOSAMPLE_ID, "
              + "FIXED_TAX_ID, "
              + "FIXED_SCIENTIFIC_NAME, "
              + "FIXED_COMMON_NAME, "
              + "FIXED, "
              + "TAX_ID, "
              + "SCIENTIFIC_NAME, "
              + "to_char(first_public, 'yyyy-mm-dd')                                                as first_public,\n"
              + "to_char(last_updated, 'yyyy-mm-dd')                                                as last_updated,\n"
              + "(select nvl(cv_broker_name.description, T1.broker_name)\n"
              + "    from XMLTable('/SAMPLE_SET[ 1 ]/SAMPLE/@broker_name' passing sample.sample_xml\n"
              + "               columns broker_name varchar(1000) path '.') T1\n"
              + "    left join cv_broker_name on (cv_broker_name.broker_name = T1.broker_name)) as broker_name,\n"
              + "(select nvl(cv_center_name.description, T2.center_name)\n"
              + "    from XMLTable('/SAMPLE_SET[ 1 ]/SAMPLE/@center_name' passing sample.sample_xml\n"
              + "                columns center_name varchar(1000) path '.') T2\n"
              + "     left join cv_center_name on (cv_center_name.center_name = T2.center_name)) as center_name\n"
              + " FROM SAMPLE "
              + "WHERE BIOSAMPLE_ID = ? fetch first row only ";
      final EraproSample sampleData =
          jdbcTemplate.queryForObject(sql, sampleRowMapper, bioSampleId);

      return sampleData;
    } catch (final IncorrectResultSizeDataAccessException e) {
      log.error(
          "Result set size expected is 1 and got more/ less that that, skipping " + bioSampleId);
    }

    return null;
  }

  private final RowMapper<EraproSample> sampleRowMapper =
      (rs, rowNum) -> {
        final EraproSample sampleBean = new EraproSample();

        sampleBean.setSampleXml(rs.getString("SAMPLE_XML"));
        sampleBean.setFirstPublic(rs.getString("FIRST_PUBLIC"));
        sampleBean.setLastUpdated(rs.getString("LAST_UPDATED"));
        sampleBean.setFirstCreated(rs.getString("FIRST_CREATED"));
        sampleBean.setStatus(rs.getInt("STATUS_ID"));
        sampleBean.setSubmissionAccountId(rs.getString("SUBMISSION_ACCOUNT_ID"));
        sampleBean.setTaxId(rs.getLong("TAX_ID"));
        sampleBean.setBiosampleId(rs.getString("BIOSAMPLE_ID"));
        sampleBean.setBrokerName(rs.getString("BROKER_NAME"));
        sampleBean.setCentreName(rs.getString("CENTER_NAME"));
        sampleBean.setFixed(rs.getString("FIXED"));
        sampleBean.setScientificName(rs.getString("SCIENTIFIC_NAME"));
        sampleBean.setFixedTaxId(rs.getString("FIXED_TAX_ID"));
        sampleBean.setFixedCommonName(rs.getString("FIXED_COMMON_NAME"));
        sampleBean.setFixedScientificName(rs.getString("FIXED_SCIENTIFIC_NAME"));

        return sampleBean;
      };
}