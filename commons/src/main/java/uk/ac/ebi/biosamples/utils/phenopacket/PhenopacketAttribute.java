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
package uk.ac.ebi.biosamples.utils.phenopacket;

public class PhenopacketAttribute {
  private String type;
  private String value;
  private String ontologyId;
  private String ontologyLabel;
  private boolean negate;

  private PhenopacketAttribute() {
    // hide constructor
  }

  public static PhenopacketAttribute build(
      final String type,
      final String value,
      final String ontologyId,
      final String ontologyLabel,
      final boolean negate) {
    final PhenopacketAttribute phenopacketAttribute = new PhenopacketAttribute();
    phenopacketAttribute.type = type;
    phenopacketAttribute.value = value;
    phenopacketAttribute.ontologyId = ontologyId;
    phenopacketAttribute.ontologyLabel = ontologyLabel;
    phenopacketAttribute.negate = negate;

    return phenopacketAttribute;
  }

  public static PhenopacketAttribute build(
      final String type, final String value, final String ontologyId, final String ontologyLabel) {
    return build(type, value, ontologyId, ontologyLabel, false);
  }

  public String getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  String getOntologyId() {
    return ontologyId;
  }

  String getOntologyLabel() {
    return ontologyLabel;
  }

  boolean isNegate() {
    return negate;
  }
}
