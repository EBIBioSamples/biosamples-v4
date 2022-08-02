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
package uk.ac.ebi.biosamples.model;

public enum AccessionType {
  ANY("SAM[END][AG]?[0-9]+"),
  ANY_GROUP("SAMEG[0-9]+"),
  ANY_SAMPLE("SAM[END][A]?[0-9]+"),
  NCBI_SAMPLE("SAMN[0-9]+"),
  EBI_SAMPLE("SAME[AG]?[0-9]+"),
  DDBJ_SAMPLE("SAMD[0-9]+");

  private final String accessionRegex;

  AccessionType(String regex) {
    this.accessionRegex = regex;
  }

  public String getAccessionRegex() {
    return this.accessionRegex;
  }

  public boolean matches(String accession) {
    return accession.matches(this.accessionRegex);
  }
}
