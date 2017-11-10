package uk.ac.ebi.biosamples.legacy.json.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.legacy.json.domain.SamplesRelations;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.legacy.json.service.SampleRelationsResourceAssembler;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/samplesrelations", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(SamplesRelations.class)
public class SamplesRelationsController {

    private final SampleRepository sampleRepository;
    private final SampleRelationsResourceAssembler relationsResourceAssembler;
    private final EntityLinks entityLinks;

    public SamplesRelationsController(SampleRepository sampleRepository,
                                      SampleRelationsResourceAssembler relationsResourceAssembler,
                                      EntityLinks entityLinks) {

        this.sampleRepository = sampleRepository;
        this.relationsResourceAssembler = relationsResourceAssembler;
        this.entityLinks = entityLinks;

    }

    @GetMapping
    public PagedResources<Resource<SamplesRelations>> allSamplesRelations(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            PagedResourcesAssembler<SamplesRelations> pagedResourcesAssembler) {


        PagedResources<Resource<Sample>> samples = sampleRepository.getPagedSamples(page, size);
        List<SamplesRelations> legacyRelationsResources = samples.getContent().stream()
                .map(Resource::getContent)
                .map(SamplesRelations::new)
                .collect(Collectors.toList());
        Pageable pageRequest = new PageRequest(page, size);
        Page<SamplesRelations> pageResources = new PageImpl<>(legacyRelationsResources, pageRequest, samples.getMetadata().getTotalElements());

        PagedResources<Resource<SamplesRelations>> pagedResources = pagedResourcesAssembler.toResource(pageResources,
                this.relationsResourceAssembler,
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
//    public ResponseEntity<Resource<SamplesRelations>> findByAccession(@PathVariable(required = false) String accessionQuery) {
    public ResponseEntity<Resource<SamplesRelations>> findByAccession(@RequestParam(required = false, defaultValue = "") String accession) {
//        String accession = accessionQuery.replaceAll("\\?accession=","");
        if (accession == null || accession.isEmpty()) {
            return ResponseEntity.notFound().build(); // Replicate v3 response code
        }
        Optional<Sample> sample = sampleRepository.findByAccession(accession);
        if (!sample.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(relationsResourceAssembler.toResource(new SamplesRelations(sample.get())));

    }







}

