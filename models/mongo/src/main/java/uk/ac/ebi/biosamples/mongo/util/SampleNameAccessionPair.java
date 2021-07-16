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
package uk.ac.ebi.biosamples.mongo.util;

public class SampleNameAccessionPair {
  private String sampleName;
  private String sampleAccession;

  public SampleNameAccessionPair(final String sampleName, final String sampleAccession) {
    this.sampleName = sampleName;
    this.sampleAccession = sampleAccession;
  }

  public void setSampleName(String sampleName) {
    this.sampleName = sampleName;
  }

  public void setSampleAccession(String sampleAccession) {
    this.sampleAccession = sampleAccession;
  }

  public String getSampleName() {
    return sampleName;
  }

  public String getSampleAccession() {
    return sampleAccession;
  }
}
