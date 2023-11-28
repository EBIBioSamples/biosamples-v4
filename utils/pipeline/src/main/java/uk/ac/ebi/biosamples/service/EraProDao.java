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
import java.util.List;
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
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Autowired
  @Qualifier("eraJdbcTemplate")
  protected JdbcTemplate jdbcTemplate;

  private static final String STATUS_CLAUSE = "STATUS_ID IN (4, 5, 6, 7, 8)";
  private static final String STATUS_CLAUSE_SUPPRESSED = "STATUS_ID IN (5, 7)";
  private static final String STATUS_CLAUSE_KILLED = "STATUS_ID IN (6, 8)";

  public List<SampleCallbackResult> doSampleCallback(
      final LocalDate minDate, final LocalDate maxDate) {
    final String query =
        "SELECT UNIQUE(BIOSAMPLE_ID), STATUS_ID, EGA_ID, LAST_UPDATED FROM SAMPLE WHERE BIOSAMPLE_ID LIKE 'SAME%' AND SAMPLE_ID LIKE 'ERS%' AND BIOSAMPLE_AUTHORITY= 'N' "
            + "AND "
            + STATUS_CLAUSE
            + " AND ((LAST_UPDATED BETWEEN ? AND ?) OR (FIRST_PUBLIC BETWEEN ? AND ?)) ORDER BY LAST_UPDATED ASC";

    final Date minDateOld = java.sql.Date.valueOf(minDate);
    final Date maxDateOld = java.sql.Date.valueOf(maxDate);

    return jdbcTemplate.query(
        query, sampleCallbackResultRowMapper, minDateOld, maxDateOld, minDateOld, maxDateOld);
  }

  public List<SampleCallbackResult> doSampleCallbackForBsdAuthoritySamples(
      final LocalDate minDate, final LocalDate maxDate) {
    final String query =
        "SELECT UNIQUE(BIOSAMPLE_ID), STATUS_ID, EGA_ID, LAST_UPDATED FROM SAMPLE WHERE BIOSAMPLE_ID LIKE 'SAME%' AND SAMPLE_ID LIKE 'ERS%' AND BIOSAMPLE_AUTHORITY= 'Y' "
            + "AND "
            + STATUS_CLAUSE
            + " AND ((LAST_UPDATED BETWEEN ? AND ?) OR (FIRST_PUBLIC BETWEEN ? AND ?)) AND EGA_ID IS NULL ORDER BY LAST_UPDATED ASC";

    final Date minDateOld = java.sql.Date.valueOf(minDate);
    final Date maxDateOld = java.sql.Date.valueOf(maxDate);

    return jdbcTemplate.query(
        query, sampleCallbackResultRowMapper, minDateOld, maxDateOld, minDateOld, maxDateOld);
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

  public List<SampleCallbackResult> doNcbiCallback(
      final LocalDate minDate, final LocalDate maxDate) {
    final String query =
        "SELECT UNIQUE(BIOSAMPLE_ID), STATUS_ID, EGA_ID, LAST_UPDATED FROM SAMPLE WHERE (BIOSAMPLE_ID LIKE 'SAMN%' OR BIOSAMPLE_ID LIKE 'SAMD%' ) AND BIOSAMPLE_AUTHORITY= 'N' "
            + "AND "
            + STATUS_CLAUSE
            + " AND ((LAST_UPDATED BETWEEN ? AND ?) OR (FIRST_PUBLIC BETWEEN ? AND ?)) ORDER BY LAST_UPDATED ASC";

    final Date minDateOld = java.sql.Date.valueOf(minDate);
    final Date maxDateOld = java.sql.Date.valueOf(maxDate);

    return jdbcTemplate.query(
        query, sampleCallbackResultRowMapper, minDateOld, maxDateOld, minDateOld, maxDateOld);
  }

  public EraproSample getSampleDetailsByBioSampleId(final String bioSampleId) {
    try {
      final String sql =
          "SELECT SAMPLE_XML, TAX_ID, SAMPLE_ID, "
              + "to_char(LAST_UPDATED, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') AS LAST_UPDATED, "
              + "to_char(FIRST_PUBLIC, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') AS FIRST_PUBLIC,  "
              + "to_char(FIRST_CREATED, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') AS FIRST_CREATED, "
              + "STATUS_ID, "
              + "SUBMISSION_ACCOUNT_ID, "
              + "BIOSAMPLE_ID, "
              + "TAX_ID, "
              + "SCIENTIFIC_NAME, "
              + "COMMON_NAME, "
              + "to_char(first_public, 'yyyy-mm-dd') as first_public,\n"
              + "to_char(last_updated, 'yyyy-mm-dd') as last_updated \n"
              + " FROM SAMPLE "
              + "WHERE BIOSAMPLE_ID = ? fetch first row only ";
      final EraproSample sampleData =
          jdbcTemplate.queryForObject(sql, eraproSampleRowMapper, bioSampleId);

      return sampleData;
    } catch (final IncorrectResultSizeDataAccessException e) {
      log.error(
          "Result set size expected is 1 and got more/ less that that, skipping " + bioSampleId);
    }

    return null;
  }

  private final RowMapper<EraproSample> eraproSampleRowMapper =
      (rs, rowNum) -> {
        final EraproSample sampleBean = new EraproSample();

        sampleBean.setSampleXml(rs.getString("SAMPLE_XML"));
        sampleBean.setSampleId(rs.getString("SAMPLE_ID"));
        sampleBean.setFirstPublic(rs.getString("FIRST_PUBLIC"));
        sampleBean.setLastUpdated(rs.getString("LAST_UPDATED"));
        sampleBean.setFirstCreated(rs.getString("FIRST_CREATED"));
        sampleBean.setStatus(rs.getInt("STATUS_ID"));
        sampleBean.setSubmissionAccountId(rs.getString("SUBMISSION_ACCOUNT_ID"));
        sampleBean.setTaxId(rs.getLong("TAX_ID"));
        sampleBean.setBiosampleId(rs.getString("BIOSAMPLE_ID"));
        sampleBean.setScientificName(rs.getString("SCIENTIFIC_NAME"));
        sampleBean.setCommonName(rs.getString("COMMON_NAME"));

        return sampleBean;
      };

  private final RowMapper<SampleCallbackResult> sampleCallbackResultRowMapper =
      (rs, rowNum) -> {
        final SampleCallbackResult sampleCallbackResult = new SampleCallbackResult();

        sampleCallbackResult.setBiosampleId(rs.getString("BIOSAMPLE_ID"));
        sampleCallbackResult.setEgaId(rs.getString("EGA_ID"));
        sampleCallbackResult.setStatusId(rs.getInt("STATUS_ID"));
        sampleCallbackResult.setLastUpdated(rs.getDate("LAST_UPDATED"));

        return sampleCallbackResult;
      };
}
