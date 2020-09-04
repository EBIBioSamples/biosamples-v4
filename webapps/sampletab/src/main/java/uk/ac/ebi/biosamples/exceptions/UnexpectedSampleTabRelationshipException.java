/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.exceptions;

public class UnexpectedSampleTabRelationshipException extends SampleTabException {
  public String sampleName;
  public String relationType;
  public String relationTarget;

  public UnexpectedSampleTabRelationshipException(
      String sampleName, String relationType, String relationTarget) {
    super(
        String.format(
            "The %s column in the SampleTab for sample %s contains an unexpected target %s",
            relationType, sampleName, relationTarget));
    this.sampleName = sampleName;
    this.relationType = relationType;
    this.relationTarget = relationTarget;
  }
}
