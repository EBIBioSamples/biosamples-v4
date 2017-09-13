package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.controller.SamplesRestController;
import uk.ac.ebi.biosamples.model.facets.Facet;
import uk.ac.ebi.biosamples.model.facets.LabelCountEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class LabelFilterResourceAssembler implements ResourceAssembler<LabelCountEntry, Resource<LabelCountEntry>> {

    private final Facet parentFacet;
    private final String searchText;
    private final String updatedAfter;
    private final String updatedBefore;
    private final String[] filters;

    LabelFilterResourceAssembler(String searchText, String updatedAfter, String updatedBefore,
                                 String[] filters, Facet parent) {
        this.searchText = searchText;
        this.updatedAfter = updatedAfter;
        this.updatedBefore = updatedBefore;
        this.filters = filters;
        this.parentFacet = parent;
    }


    @Override
    public Resource<LabelCountEntry> toResource(LabelCountEntry entity) {
        Resource<LabelCountEntry> resource = new Resource<>(entity);


        /* Here I need to take into account the logic:
            1. If the filters already contains the more general filter, this link should remove that filter
            In order to do that though, I need to have also the facet of which this label is part of
            // TODO: I need a way of translating from a filter to its parts (Attribute:Organism:Homo sapiens -> Attribute, Organism, Homo Sapiens)
            2. If the filter is already applied, the link should not be built?
            3. Potentially we should toggle the filter?
         */
        List<String> newFilters = new ArrayList<>();
//        newFilters.addAll(Arrays.stream(filters).map(this::getUTF8Encoding).collect(Collectors.toList()));
//        newFilters.add(getUTF8Encoding(parentFacet.getType().getFacetId() + ":" + parentFacet.getLabel() + ":" + entity.label));
        newFilters.addAll(Arrays.asList(filters));
        newFilters.add(parentFacet.getType().getFacetId() + ":" + parentFacet.getLabel() + ":" + entity.label);


        Link filterLink = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(SamplesRestController.class)
                .searchHal(searchText, updatedAfter, updatedBefore, newFilters.toArray(new String[newFilters.size()]), null, null)).withRel("filter");
        resource.add(filterLink);
        return resource;
    }

//    private String getUTF8Encoding(String string) {
//        try {
//            return URLEncoder.encode(string, "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
