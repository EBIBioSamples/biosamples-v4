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

public class DuplicateDomainSampleException extends SampleTabException {

  private static final long serialVersionUID = -3469688972274912777L;
  public final String domain;
  public final String name;

  public DuplicateDomainSampleException(String domain, String name) {
    super("Multiple existing accessions of domain '" + domain + "' sample name '" + name + "'");
    this.domain = domain;
    this.name = name;
  }
}
