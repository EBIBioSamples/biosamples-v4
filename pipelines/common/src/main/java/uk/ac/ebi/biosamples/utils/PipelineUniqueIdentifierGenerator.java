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
package uk.ac.ebi.biosamples.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import uk.ac.ebi.biosamples.model.PipelineName;

public class PipelineUniqueIdentifierGenerator {
  public static String getPipelineUniqueIdentifier(final PipelineName pipelineName) {
    final LocalDate localDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

    return localDate.getDayOfMonth()
        + String.valueOf(localDate.getMonthValue())
        + localDate.getYear()
        + "-"
        + pipelineName;
  }
}
