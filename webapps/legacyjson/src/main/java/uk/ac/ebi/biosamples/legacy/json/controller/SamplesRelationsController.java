package uk.ac.ebi.biosamples.legacy.json.controller;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.core.EmbeddedWrapper;
import org.springframework.hateoas.core.EmbeddedWrappers;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.legacy.json.domain.ExternalLinksRelation;
import uk.ac.ebi.biosamples.legacy.json.domain.GroupsRelations;
import uk.ac.ebi.biosamples.legacy.json.domain.SamplesRelations;
import uk.ac.ebi.biosamples.legacy.json.repository.RelationsRepository;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.legacy.json.service.ExternalLinksResourceAssembler;
import uk.ac.ebi.biosamples.legacy.json.service.GroupRelationsResourceAssembler;
import uk.ac.ebi.biosamples.legacy.json.service.SampleRelationsResourceAssembler;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;

@RestController
@RequestMapping(value = "/samplesrelations", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(SamplesRelations.class)
public class SamplesRelationsController {

    private final EntityLinks entityLinks;
    private final SampleRepository sampleRepository;
    private final RelationsRepository relationsRepository;
    private final GroupRelationsResourceAssembler groupRelationsResourceAssembler;
    private final SampleRelationsResourceAssembler sampleRelationsResourceAssembler;
    private final ExternalLinksResourceAssembler externalLinksResourceAssembler;
    private final PagedResourcesAssembler<SamplesRelations> pagedResourcesAssembler;

    public SamplesRelationsController(EntityLinks entityLinks,
                                      SampleRepository sampleRepository,
                                      RelationsRepository relationsRepository,
                                      GroupRelationsResourceAssembler groupRelationsResourceAssembler,
                                      SampleRelationsResourceAssembler sampleRelationsResourceAssembler, ExternalLinksResourceAssembler externalLinksResourceAssembler, PagedResourcesAssembler<SamplesRelations> pagedResourcesAssembler) {

        this.entityLinks = entityLinks;
        this.sampleRepository = sampleRepository;
        this.relationsRepository = relationsRepository;
        this.externalLinksResourceAssembler = externalLinksResourceAssembler;
        this.groupRelationsResourceAssembler = groupRelationsResourceAssembler;
        this.sampleRelationsResourceAssembler = sampleRelationsResourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @GetMapping("/{accession:SAM[END]A?\\d+}")
    public ResponseEntity<Resource<SamplesRelations>> relationsOfSample(@PathVariable String accession) {
        Optional<Sample> sample = sampleRepository.findByAccession(accession);
        if (!sample.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(sampleRelationsResourceAssembler.toResource(new SamplesRelations(sample.get())));
    }

    @GetMapping("/{accession:SAM[END]A?\\d+}/groups")
    public ResponseEntity<Resources<GroupsRelations>> getSamplesGroupRelations(@PathVariable String accession) {

        List<Resource> associatedGroups = relationsRepository
                .getGroupsRelationships(accession).stream()
                .map(groupRelationsResourceAssembler::toResource)
                .collect(Collectors.toList());

        Link selfLink = linkTo(methodOn(this.getClass()).getSamplesGroupRelations(accession)).withSelfRel();
        Resources responseBody = new Resources(wrappedCollection(associatedGroups, GroupsRelations.class), selfLink);
        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/{accession:SAM[END]A?\\d+}/{relationType}")
    public ResponseEntity<Resources> getSamplesRelations(
            @PathVariable String accession,
            @PathVariable String relationType) {

        if (!relationsRepository.isSupportedSamplesRelation(relationType)) {
            return ResponseEntity.badRequest().build();
        }

        List<Resource> associatedSamples = relationsRepository
                .getSamplesRelations(accession, relationType).stream()
                .map(sampleRelationsResourceAssembler::toResource)
                .collect(Collectors.toList());

        Link selfLink = linkTo(methodOn(this.getClass()).getSamplesRelations(accession, relationType)).withSelfRel();
        Resources responseBody = new Resources(wrappedCollection(associatedSamples, SamplesRelations.class), selfLink);
        return ResponseEntity.ok(responseBody);

    }

    @GetMapping("/{accession:SAM[END]A?\\d+}/externalLinks")
    public ResponseEntity<Resources> getSamplesExternalLinks(
            @PathVariable String accession) {

        Optional<Sample> sample = sampleRepository.findByAccession(accession);
        if (!sample.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        List<Resource> exteranlLinksResources = sample.get().getExternalReferences().stream()
                .map(ExternalReference::getUrl)
                .map(ExternalLinksRelation::new)
                .map(externalLinksResourceAssembler::toResource)
                .collect(Collectors.toList());

        Link selfLink = linkTo(methodOn(this.getClass()).getSamplesExternalLinks(accession)).withSelfRel();
        Resources responseBody = new Resources(wrappedCollection(exteranlLinksResources, ExternalLinksRelation.class), selfLink);
        return ResponseEntity.ok(responseBody);

    }


    /**
     * Wrap the collection and return an empty _embedded if provided collection is empty
     * @param resourceCollection The collection to wrap
     * @param collectionClass the class to use to create the empty collection wrapper
     * @return A resource collection
     */
    private Collection wrappedCollection(List<Resource> resourceCollection, Class collectionClass) {
        EmbeddedWrappers wrappers = new EmbeddedWrappers(false);
        EmbeddedWrapper wrapper;
        if (resourceCollection.isEmpty())
            wrapper = wrappers.emptyCollectionOf(collectionClass);
        else
            wrapper = wrappers.wrap(resourceCollection);
        return Collections.singletonList(wrapper);
    }

    @GetMapping
    public PagedResources<Resource<SamplesRelations>> allSamplesRelations(
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
            @RequestParam(value = "sort", required = false, defaultValue = "asc") String sort) {

        PagedResources<Resource<Sample>> samples = sampleRepository.findSamples(page, size);
        List<SamplesRelations> legacyRelationsResources = samples.getContent().stream()
                .map(Resource::getContent)
                .map(SamplesRelations::new)
                .collect(Collectors.toList());
        Pageable pageRequest = new PageRequest(page, size);
        Page<SamplesRelations> pageResources = new PageImpl<>(legacyRelationsResources, pageRequest, samples.getMetadata().getTotalElements());

        PagedResources<Resource<SamplesRelations>> pagedResources = pagedResourcesAssembler.toResource(pageResources,
                this.sampleRelationsResourceAssembler,
                entityLinks.linkToCollectionResource(SamplesRelations.class));

        pagedResources.add(linkTo(methodOn(SamplesRelationsController.class).searchMethods()).withRel("search"));

        return pagedResources;

    }

    @GetMapping("/search")
    public Resources searchMethods() {
        Resources resources = Resources.wrap(Collections.emptyList());
        resources.add(linkTo(methodOn(this.getClass()).searchMethods()).withSelfRel());
        resources.add(linkTo(methodOn(this.getClass()).findByAccession(null)).withRel("findOneByAccession"));

        return resources;
    }

    @GetMapping("/search/findOneByAccession") // Replicate v3 way of working
    public ResponseEntity<Resource<SamplesRelations>> findByAccession(@RequestParam(required = false, defaultValue = "") String accession) {
        if (accession == null || accession.isEmpty()) {
            return ResponseEntity.notFound().build(); // Replicate v3 response code
        }
        Optional<Sample> sample = sampleRepository.findByAccession(accession);
        if (!sample.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(sampleRelationsResourceAssembler.toResource(new SamplesRelations(sample.get())));

    }







}

