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
package uk.ac.ebi.biosamples.ena;

public class EnaDatabaseSample {
  public String lastUpdated;
  public String firstPublic;
  public String brokerName;
  public String bioSamplesId;
  public String centreName;
  public String fixed;
  public String taxId;
  public String scientificName;
  public String fixedTaxId;
  public String fixedCommonName;
  public String fixedScientificName;

  @Override
  public String toString() {
    return "EnaDatabaseSample{"
        + "lastUpdated='"
        + lastUpdated
        + '\''
        + ", firstPublic='"
        + firstPublic
        + '\''
        + ", brokerName='"
        + brokerName
        + '\''
        + ", bioSamplesId='"
        + bioSamplesId
        + '\''
        + ", centreName='"
        + centreName
        + '\''
        + ", fixed='"
        + fixed
        + '\''
        + ", taxId='"
        + taxId
        + '\''
        + ", scientificName='"
        + scientificName
        + '\''
        + ", fixedTaxId='"
        + fixedTaxId
        + '\''
        + ", fixedCommonName='"
        + fixedCommonName
        + '\''
        + ", fixedScientificName='"
        + fixedScientificName
        + '\''
        + '}';
  }
}
