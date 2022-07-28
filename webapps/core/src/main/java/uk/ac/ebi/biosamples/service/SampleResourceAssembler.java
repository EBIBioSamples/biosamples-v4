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

import java.util.List;
import java.util.Optional;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.controller.SampleCurationLinksRestController;
import uk.ac.ebi.biosamples.controller.SampleRestController;
import uk.ac.ebi.biosamples.controller.StructuredDataRestController;
import uk.ac.ebi.biosamples.model.Sample;

/**
 * This class is used by Spring to add HAL _links for {@Link Sample} objects.
 *
 * @author faulcon
 */
@Service
public class SampleResourceAssembler
    implements RepresentationModelAssembler<Sample, EntityModel<Sample>> {

  public static final String REL_CURATIONDOMAIN = "curationDomain";
  public static final String REL_CURATIONLINKS = "curationLinks";
  public static final String REL_CURATIONLINK = "curationLink";

  public SampleResourceAssembler() {}

  private Link getSelfLink(
      String accession,
      Optional<Boolean> legacydetails,
      Optional<List<String>> curationDomains,
      Class controllerClass) {
    UriComponentsBuilder uriComponentsBuilder =
        linkTo(controllerClass, accession).toUriComponentsBuilder();
    if (legacydetails.isPresent() && legacydetails.get()) {
      uriComponentsBuilder.queryParam("legacydetails", legacydetails);
    }
    if (curationDomains != null && curationDomains.isPresent()) {
      if (curationDomains.get().size() == 0) {
        uriComponentsBuilder.queryParam("curationdomain", (Object[]) null);
      } else {
        for (String curationDomain : curationDomains.get()) {
          uriComponentsBuilder.queryParam("curationdomain", curationDomain);
        }
      }
    }
    return new Link(uriComponentsBuilder.build().toUriString(), Link.REL_SELF);
  }

  private Link getCurationDomainLink(Link selfLink) {
    UriComponents selfUriComponents =
        UriComponentsBuilder.fromUriString(selfLink.getHref()).build();
    if (selfUriComponents.getQueryParams().size() == 0) {
      return new Link(selfLink.getHref() + "{?curationdomain}", REL_CURATIONDOMAIN);
    } else {
      return new Link(selfLink.getHref() + "{&curationdomain}", REL_CURATIONDOMAIN);
    }
  }

  private Link getCurationLinksLink(String accession) {
    return linkTo(
            methodOn(SampleCurationLinksRestController.class)
                .getCurationLinkPageJson(accession, null, null))
        .withRel("curationLinks");
  }

  private Link getCurationLinkLink(String accession) {
    return linkTo(
            methodOn(SampleCurationLinksRestController.class).getCurationLinkJson(accession, null))
        .withRel("curationLink");
  }

  private Link getStructuredDataLink(String accession) {
    return linkTo(methodOn(StructuredDataRestController.class).get(accession))
        .withRel("structuredData");
  }

  public EntityModel<Sample> toModel(
      Sample sample,
      Optional<Boolean> legacydetails,
      Optional<List<String>> curationDomains,
      Class controllerClass) {
    EntityModel<Sample> sampleResource = new EntityModel<>(sample);
    sampleResource.add(
        getSelfLink(sample.getAccession(), legacydetails, curationDomains, controllerClass));
    // add link to select curation domain
    sampleResource.add(getCurationDomainLink(sampleResource.getLink(Link.REL_SELF).get()));
    // add link to curationLinks on this sample
    sampleResource.add(getCurationLinksLink(sample.getAccession()));
    sampleResource.add(getCurationLinkLink(sample.getAccession()));
    sampleResource.add(getStructuredDataLink(sample.getAccession()));
    return sampleResource;
  }

  public EntityModel<Sample> toModel(
      Sample sample, Optional<Boolean> legacydetails, Optional<List<String>> curationDomains) {
    Class controllerClass = SampleRestController.class;
    return toModel(sample, legacydetails, curationDomains, controllerClass);
  }

  public EntityModel<Sample> toModel(Sample sample, Class controllerClass) {
    return toModel(sample, Optional.empty(), Optional.empty(), controllerClass);
  }

  @Override
  public EntityModel<Sample> toModel(Sample sample) {
    Class controllerClass = SampleRestController.class;
    return toModel(sample, controllerClass);
  }
}
