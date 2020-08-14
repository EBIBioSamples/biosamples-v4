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
package uk.ac.ebi.biosamples.legacy.json.repository;

import java.util.Collections;
import java.util.Optional;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;

@Service
public class SampleRepository {

  private final BioSamplesClient client;

  private final Filter GROUP_FILTER = FilterBuilder.create().onAccession("SAMEG[0-9]+").build();
  private final Filter SAMPLE_FILTER =
      FilterBuilder.create().onAccession("SAM(N|D|EA|E)[0-9]+").build();

  public SampleRepository(BioSamplesClient client) {
    this.client = client;
  }

  public Optional<Sample> findByAccession(String accession) {

    return client.fetchSample(accession);
  }

  /**
   * Search for an optional sample resource associated with a group
   *
   * @param groupAccession the group accession
   * @return Optional sample resource
   */
  public Optional<Resource<Sample>> findFirstSampleByGroup(String groupAccession) {

    PagedResources<Resource<Sample>> resourcePage = findSamplesByGroup(groupAccession, 0, 1);

    if (resourcePage.getContent().isEmpty()) {
      return Optional.empty();
    }

    return resourcePage.getContent().stream().findFirst();
  }

  public PagedResources<Resource<Sample>> findSamplesByGroup(
      String groupAccession, int page, int size) {
    return this.findSamplesByTextAndGroup("*:*", groupAccession, page, size);
  }

  public PagedResources<Resource<Sample>> findSamples(int page, int size) {

    return client.fetchPagedSampleResource(
        "*:*", Collections.singletonList(SAMPLE_FILTER), page, size);
  }

  public PagedResources<Resource<Sample>> findGroups(int page, int size) {
    return client.fetchPagedSampleResource(
        "*:*", Collections.singletonList(GROUP_FILTER), page, size);
  }

  public PagedResources<Resource<Sample>> findSamplesByText(String text, int page, int size) {
    return client.fetchPagedSampleResource(
        text, Collections.singletonList(SAMPLE_FILTER), page, size);
  }

  public PagedResources<Resource<Sample>> findGroupsByText(String text, int page, int size) {
    return client.fetchPagedSampleResource(
        text, Collections.singletonList(GROUP_FILTER), page, size);
  }

  public PagedResources<Resource<Sample>> findSamplesByTextAndGroup(
      String text, String groupAccession, int page, int size) {

    return client.fetchPagedSampleResource(
        text, Collections.singletonList(groupMemberFilter(groupAccession)), page, size);
  }

  public PagedResources<Resource<Sample>> findSampleInGroup(
      String sampleAccession, String groupAccession) {
    return findSamplesByTextAndGroup(sampleAccession, groupAccession, 0, 1);
  }

  private Filter groupMemberFilter(String groupAccession) {
    return FilterBuilder.create().onInverseRelation("has member").withValue(groupAccession).build();
  }
}
