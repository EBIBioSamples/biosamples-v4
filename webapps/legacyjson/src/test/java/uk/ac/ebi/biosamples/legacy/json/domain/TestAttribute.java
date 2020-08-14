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
package uk.ac.ebi.biosamples.legacy.json.domain;

import uk.ac.ebi.biosamples.model.Attribute;

public class TestAttribute {

  public final String type;
  public final String value;
  private String ontologyUri;
  private String unit;

  public TestAttribute(String type, String value) {
    this.type = type;
    this.value = value;
    this.ontologyUri = "";
    this.unit = "";
  }

  public TestAttribute withOntologyUri(String ontologyUri) {
    this.ontologyUri = ontologyUri;
    return this;
  }

  public TestAttribute withUnit(String unit) {
    this.unit = unit;
    return this;
  }

  public Attribute build() {
    return Attribute.build(this.type, this.value, this.ontologyUri, this.unit);
  }
}
