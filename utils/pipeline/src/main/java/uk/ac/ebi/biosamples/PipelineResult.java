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
package uk.ac.ebi.biosamples;

public class PipelineResult {
  private final long modifiedRecords;
  private final boolean success;
  private final String accession;

  public PipelineResult(String accession, long modifiedRecords, boolean success) {
    this.accession = accession;
    this.modifiedRecords = modifiedRecords;
    this.success = success;
  }

  public String getAccession() {
    return accession;
  }

  public long getModifiedRecords() {
    return modifiedRecords;
  }

  public boolean isSuccess() {
    return success;
  }
}
