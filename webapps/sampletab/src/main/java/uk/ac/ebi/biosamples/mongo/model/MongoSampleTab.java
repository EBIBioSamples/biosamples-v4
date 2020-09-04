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
package uk.ac.ebi.biosamples.mongo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class MongoSampleTab {

  @Id @JsonIgnore private String id;
  private String domain;
  private String sampleTab;

  @Indexed(background = true)
  private Collection<String> accessions;

  private MongoSampleTab(
      String id, String domain, String sampleTab, Collection<String> accessions) {
    this.id = id;
    this.sampleTab = sampleTab;
    this.accessions = accessions;
  }

  public String getId() {
    return id;
  }

  public String getDomain() {
    return domain;
  }

  public String getSampleTab() {
    return sampleTab;
  }

  public Collection<String> getAccessions() {
    return Collections.unmodifiableCollection(accessions);
  }

  @JsonCreator
  public static MongoSampleTab build(
      String id, String domain, String sampleTab, Collection<String> accessions) {
    // mongo won't allow a new line character in a string, so escape them
    sampleTab = sampleTab.replace("\n", "\\n");
    SortedSet<String> sortedAccessions = new TreeSet<>();
    for (String accession : accessions) {
      if (accession != null) {
        sortedAccessions.add(accession);
      }
    }
    return new MongoSampleTab(
        id, domain, sampleTab, Collections.unmodifiableCollection(sortedAccessions));
  }
}
