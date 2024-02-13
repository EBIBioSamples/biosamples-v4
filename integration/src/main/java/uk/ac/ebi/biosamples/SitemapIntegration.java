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
package uk.ac.ebi.biosamples;

import java.net.URI;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.utils.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.XmlSitemap;
import uk.ac.ebi.biosamples.model.XmlSitemapIndex;
import uk.ac.ebi.biosamples.model.XmlUrlSet;

@Order
@Component
public class SitemapIntegration extends AbstractIntegration {
  private final URI biosamplesSubmissionUri;
  private final RestOperations restTemplate;

  @Value("${model.page.size:10}")
  private int sitemapPageSize;

  public SitemapIntegration(
      final BioSamplesClient client,
      final RestTemplateBuilder restTemplateBuilder,
      final BioSamplesProperties bioSamplesProperties) {
    super(client);
    biosamplesSubmissionUri = bioSamplesProperties.getBiosamplesClientUri();
    restTemplate = restTemplateBuilder.build();
  }

  @Override
  protected void phaseOne() {}

  @Override
  protected void phaseTwo() {
    final List<EntityModel<Sample>> samples = new ArrayList<>();
    final Map<String, Boolean> lookupTable = new HashMap<>();
    for (final EntityModel<Sample> sample : publicClient.fetchSampleResourceAll()) {
      samples.add(sample);
      lookupTable.put(Objects.requireNonNull(sample.getContent()).getAccession(), Boolean.FALSE);
    }

    if (samples.size() <= 0) {
      throw new RuntimeException("No search results found!");
    }

    final int expectedSitemapIndexSize = Math.floorDiv(samples.size(), sitemapPageSize) + 1;

    final XmlSitemapIndex index = getSitemapIndex();
    if (index.getXmlSitemaps().size() != expectedSitemapIndexSize) {
      throw new RuntimeException(
          "The model index size ("
              + index.getXmlSitemaps().size()
              + ") doesn't match the expected size ("
              + expectedSitemapIndexSize
              + ")");
    }

    for (final XmlSitemap sitemap : index.getXmlSitemaps()) {
      final XmlUrlSet urlSet = getUrlSet(sitemap);
      urlSet
          .getXmlUrls()
          .forEach(
              xmlUrl -> {
                final UriComponents sampleUri =
                    UriComponentsBuilder.fromPath(xmlUrl.getLoc()).build();
                final String sampleAccession = getAccessionFromUri(sampleUri);
                lookupTable.replace(sampleAccession, Boolean.TRUE);
              });
    }
    lookupTable.entrySet().stream()
        .filter(entry -> !entry.getValue())
        .findFirst()
        .ifPresent(
            entry -> {
              throw new RuntimeException("Sample " + entry.getKey() + " is not in the sitemap");
            });
  }

  @Override
  protected void phaseThree() {}

  @Override
  protected void phaseFour() {}

  @Override
  protected void phaseFive() {}

  @Override
  protected void phaseSix() {}

  private String getAccessionFromUri(final UriComponents uri) {
    final List<String> pathSegments = uri.getPathSegments();
    return pathSegments.get(pathSegments.size() - 1);
  }

  private XmlSitemapIndex getSitemapIndex() {
    final UriComponentsBuilder builder = UriComponentsBuilder.fromUri(biosamplesSubmissionUri);
    final UriComponents sitemapUri = builder.pathSegment("sitemap").build();
    final ResponseEntity<XmlSitemapIndex> responseEntity =
        restTemplate.getForEntity(sitemapUri.toUri(), XmlSitemapIndex.class);
    if (!responseEntity.getStatusCode().is2xxSuccessful()) {
      throw new RuntimeException("Sitemap not available");
    }
    return responseEntity.getBody();
  }

  private XmlUrlSet getUrlSet(final XmlSitemap sitemap) {
    final ResponseEntity<XmlUrlSet> urlSetReponseEntity =
        restTemplate.getForEntity(sitemap.getLoc(), XmlUrlSet.class);
    if (!urlSetReponseEntity.getStatusCode().is2xxSuccessful()) {
      throw new RuntimeException("Unable to reach a model urlset");
    }
    return urlSetReponseEntity.getBody();
  }
}
