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
package uk.ac.ebi.biosamples.ega;

import java.util.List;
import lombok.Data;

@Data
class EgaResponse {
  private Response response;
}

@Data
class Response {
  private long numTotalResults;
  private List<Result> result;
}

@Data
class Result {
  private String egaStableId;
  private String centerName;
  private String title;
  private String bioSampleId;
  private String subjectId;
  private String gender;
  private String phenotype;
}
