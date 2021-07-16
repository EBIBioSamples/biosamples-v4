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
package uk.ac.ebi.biosamples.livelist;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class LiveListPipelineDao {
  @Autowired
  @Qualifier("eraJdbcTemplate")
  protected JdbcTemplate jdbcTemplate;

  private Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Returns SUPPRESSED ENA samples
   *
   * @return
   */
  public List<String> doGetSuppressedEnaSamples() {
    String query =
        "SELECT UNIQUE(BIOSAMPLE_ID) FROM SAMPLE WHERE BIOSAMPLE_ID LIKE 'SAME%' AND SAMPLE_ID LIKE 'ERS%' AND BIOSAMPLE_AUTHORITY= 'N' AND STATUS_ID = 5";

    return jdbcTemplate.queryForList(query, String.class);
  }

  /**
   * Returns KILLED ENA samples
   *
   * @return
   */
  public List<String> doGetKilledEnaSamples() {
    String query =
        "SELECT UNIQUE(BIOSAMPLE_ID) FROM SAMPLE WHERE BIOSAMPLE_ID LIKE 'SAME%' AND SAMPLE_ID LIKE 'ERS%' AND BIOSAMPLE_AUTHORITY= 'N' AND STATUS_ID IN (6, 8)";

    return jdbcTemplate.queryForList(query, String.class);
  }
}
