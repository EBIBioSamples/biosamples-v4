package uk.ac.ebi.biosamples.legacy.json.controller;

import org.springframework.hateoas.*;
import org.springframework.hateoas.core.EmbeddedWrapper;
import org.springframework.hateoas.core.EmbeddedWrappers;
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

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


//        PagedResources<Resource<Sample>> samples = sampleRepository.findSamples(page, size);
//        List<SamplesRelations> legacyRelationsResources = samples.getContent().stream()
//                .map(Resource::getContent)
//                .map(SamplesRelations::new)
//                .collect(Collectors.toList());
//        Pageable pageRequest = new PageRequest(page, size);
//        Page<SamplesRelations> pageResources = new PageImpl<>(legacyRelationsResources, pageRequest, samples.getMetadata().getTotalElements());
//        PagedResources<Resource<ExternalLinksRelation>> pagedResources = pagedResourcesAssembler.toResource(pageResources,
//                this.externalLinksResourceAssembler,
//                entityLinks.linkToCollectionResource(ExternalLinksRelation.class));
//        pagedResources.add(linkTo(methodOn(ExternalLinksRelationsController.class).searchMethods()).withRel("search"));

        return pagedResourcesConverter.toExternalLinksRelationPagedResource(null);

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










}

