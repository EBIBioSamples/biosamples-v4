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

public class DomainOwnershipException extends SampleTabException {

  public final String domain;
  public final String accession;

  public DomainOwnershipException(String domain, String accession) {
    super("Sample with accession " + accession + " is not part of the domain " + domain);
    this.domain = domain;
    this.accession = accession;
  }
}
