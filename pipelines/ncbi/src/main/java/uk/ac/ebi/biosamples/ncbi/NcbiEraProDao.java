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
package uk.ac.ebi.biosamples.ncbi;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Lazy
@Service
public class NcbiEraProDao {
  @Autowired
  @Qualifier("eraJdbcTemplate")
  protected JdbcTemplate jdbcTemplate;

  public String getSraAccession(final String sampleAccession) {
    String sql = "SELECT SAMPLE_ID FROM SAMPLE WHERE BIOSAMPLE_ID = ? ";

    List<String> resultList =
        jdbcTemplate.queryForList(sql, new Object[] {sampleAccession}, String.class);

    if (resultList != null && resultList.size() > 0) return resultList.get(0);
    else return null;
  }
}
