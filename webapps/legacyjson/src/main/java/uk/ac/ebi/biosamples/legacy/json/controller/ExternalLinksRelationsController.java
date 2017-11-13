package uk.ac.ebi.biosamples.legacy.json.controller;

import org.springframework.hateoas.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.legacy.json.domain.ExternalLinksRelation;
import uk.ac.ebi.biosamples.legacy.json.repository.RelationsRepository;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.legacy.json.service.ExternalLinksResourceAssembler;
import uk.ac.ebi.biosamples.legacy.json.service.PagedResourcesConverter;

import java.util.Collections;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/externallinksrelations", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(ExternalLinksRelation.class)
public class ExternalLinksRelationsController {

    private final EntityLinks entityLinks;
    private final SampleRepository sampleRepository;
    private final RelationsRepository relationsRepository;
    private final PagedResourcesConverter pagedResourcesConverter;
    private final ExternalLinksResourceAssembler externalLinksResourceAssembler;

    public ExternalLinksRelationsController(EntityLinks entityLinks,
                                            SampleRepository sampleRepository,
                                            RelationsRepository relationsRepository,
                                            PagedResourcesConverter pagedResourcesConverter, ExternalLinksResourceAssembler externalLinksResourceAssembler) {


        this.entityLinks = entityLinks;
        this.sampleRepository = sampleRepository;
        this.relationsRepository = relationsRepository;
        this.pagedResourcesConverter = pagedResourcesConverter;
        this.externalLinksResourceAssembler = externalLinksResourceAssembler;
    }


    @GetMapping
    public PagedResources<Resource<ExternalLinksRelation>> allSamplesRelations(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {

        PagedResources pagedResources = pagedResourcesConverter.toExternalLinksRelationPagedResource(null);
        pagedResources.add(linkTo(methodOn(ExternalLinksRelationsController.class).searchMethods()).withRel("search"));
        return pagedResources;

    }

    public Resources searchMethods() {
        Resources resources = Resources.wrap(Collections.emptyList());
        resources.add(linkTo(methodOn(this.getClass()).searchMethods()).withSelfRel());

        return resources;
    }









}

