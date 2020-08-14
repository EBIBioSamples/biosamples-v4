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
package uk.ac.ebi.biosamples;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IntegrationProperties {

  @Value("${biosamples.submissionuri.sampletab:http://localhost:8082}")
  private URI biosampleSubmissionUriSampletab;

  @Value("${biosamples.legacyxml.uri:http://localhost:8083}")
  private URI biosamplesLegacyXMLUri;

  @Value("${biosamples.legacyjson.uri:http://localhost:8084}")
  private URI biosamplesLegacyJSONUri;

  @Value("${biosamples.legacyapikey:#{null}}")
  private String legacyApiKey;

  public URI getBiosampleSubmissionUriSampleTab() {
    return biosampleSubmissionUriSampletab;
  }

  public URI getBiosamplesLegacyXMLUri() {
    return biosamplesLegacyXMLUri;
  }

  public String getLegacyApiKey() {
    return legacyApiKey;
  }

  public URI getBiosamplesLegacyJSONUri() {
    return biosamplesLegacyJSONUri;
  }
}
