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
package uk.ac.ebi.biosamples.service;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Optional;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.controller.SampleCurationLinksController;
import uk.ac.ebi.biosamples.controller.SampleRestController;
import uk.ac.ebi.biosamples.controller.StructuredDataController;
import uk.ac.ebi.biosamples.core.model.Sample;

/**
 * This class is used by Spring to add HAL _links for {@Link Sample} objects.
 *
 * @author faulcon
 */
@Service
public class SampleResourceAssembler
    implements RepresentationModelAssembler<Sample, EntityModel<Sample>> {
  public SampleResourceAssembler() {}

  private Link getSelfLink(
      final String accession, final Optional<Boolean> legacydetails, final Class controllerClass) {
    final UriComponentsBuilder uriComponentsBuilder =
        linkTo(controllerClass, accession).toUriComponentsBuilder();
    if (legacydetails.isPresent() && legacydetails.get()) {
      uriComponentsBuilder.queryParam("legacydetails", legacydetails);
    }

    return Link.of(uriComponentsBuilder.build().toUriString(), IanaLinkRelations.SELF);
  }

  private Link getApplyCurationLink(final Link selfLink) {
    final UriComponents selfUriComponents =
        UriComponentsBuilder.fromUriString(selfLink.getHref()).build();

    if (selfUriComponents.getQueryParams().isEmpty()) {
      return Link.of(selfLink.getHref() + "{?applyCurations}", String.valueOf(false))
          .withRel("applyCurations");
    } else {
      return Link.of(selfLink.getHref() + "{&applyCurations}", String.valueOf(false))
          .withRel("applyCurations");
    }
  }

  private Link getCurationLinksLink(final String accession) {
    return linkTo(
            methodOn(SampleCurationLinksController.class)
                .getCurationLinkPageJson(accession, null, null))
        .withRel("curationLinks");
  }

  private Link getCurationLinkLink(final String accession) {
    return linkTo(
            methodOn(SampleCurationLinksController.class).getCurationLinkJson(accession, null))
        .withRel("curationLink");
  }

  private Link getStructuredDataLink(final String accession) {
    return linkTo(methodOn(StructuredDataController.class).get(accession))
        .withRel("structuredData");
  }

  private EntityModel<Sample> toModel(
      final Sample sample,
      final Optional<Boolean> legacydetails,
      final boolean applyCurations,
      final Class controllerClass) {
    final EntityModel<Sample> sampleResource = EntityModel.of(sample);

    sampleResource.add(getSelfLink(sample.getAccession(), legacydetails, controllerClass));
    // add link to apply curations
    sampleResource.add(getApplyCurationLink(sampleResource.getLink(IanaLinkRelations.SELF).get()));
    // add link to curationLinks on this sample
    sampleResource.add(getCurationLinksLink(sample.getAccession()));
    sampleResource.add(getCurationLinkLink(sample.getAccession()));
    sampleResource.add(getStructuredDataLink(sample.getAccession()));

    return sampleResource;
  }

  public EntityModel<Sample> toModel(
      final Sample sample, final Optional<Boolean> legacydetails, final boolean applyCurations) {
    return toModel(sample, legacydetails, applyCurations, SampleRestController.class);
  }

  public EntityModel<Sample> toModel(final Sample sample, final Class controllerClass) {
    return toModel(sample, Optional.empty(), true, controllerClass);
  }

  @Override
  public EntityModel<Sample> toModel(final Sample sample) {
    return toModel(sample, SampleRestController.class);
  }
}
