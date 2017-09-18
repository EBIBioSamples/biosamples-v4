package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import uk.ac.ebi.biosamples.controller.SamplesRestController;
import uk.ac.ebi.biosamples.model.facets.LabelCountEntry;
import uk.ac.ebi.biosamples.model.facets.StringListFacet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FacetResourceAssembler {

    public static class LabelFilterResourceAssembler implements ResourceAssembler<LabelCountEntry, Resource<LabelCountEntry>> {

        private final StringListFacet parentFacet;
        private final String searchText;
        private final String updatedAfter;
        private final String updatedBefore;
        private final String[] filters;

        LabelFilterResourceAssembler(String searchText, String updatedAfter, String updatedBefore,
                                     String[] filters, StringListFacet parent) {
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
            newFilters.addAll(Arrays.asList(filters));
            newFilters.add(parentFacet.getType().getFacetId() + ":" + parentFacet.getLabel() + ":" + entity.label);


            Link filterLink = ControllerLinkBuilder.linkTo(
                    ControllerLinkBuilder.methodOn(SamplesRestController.class)
                            .searchHal(searchText, updatedAfter, updatedBefore, newFilters.toArray(new String[newFilters.size()]), null, null)).withRel("filter");
            resource.add(filterLink);
            return resource;
        }
    }

    public static class StringListFacetResourceAssembler implements ResourceAssembler<StringListFacet, Resource<StringListFacet>> {

        private final StringListFacet facet;
        private final String searchText;
        private final String updatedAfter;
        private final String updatedBefore;
        private final String[] filters;

        StringListFacetResourceAssembler(String searchText, String updatedAfter, String updatedBefore,
                                     String[] filters, StringListFacet facet) {
            this.searchText = searchText;
            this.updatedAfter = updatedAfter;
            this.updatedBefore = updatedBefore;
            this.filters = filters;
            this.facet = facet;
        }


        @Override
        public Resource<StringListFacet> toResource(StringListFacet entity) {
            Resource<StringListFacet> resource = new Resource<>(entity);

            String generalFilterName = String.format("%s:%s", facet.getType().getFacetId(), facet.getLabel());
            List<String> validFilters = Arrays.stream(filters).filter(specificFilter -> !specificFilter.startsWith(generalFilterName)).collect(Collectors.toList());;
            validFilters.add(generalFilterName);



            Link filterLink = ControllerLinkBuilder.linkTo(
                    ControllerLinkBuilder.methodOn(SamplesRestController.class)
                            .searchHal(searchText, updatedAfter, updatedBefore, validFilters.toArray(new String[validFilters.size()]), null, null)).withRel("filter");
            resource.add(filterLink);
            return resource;
        }
    }
}
