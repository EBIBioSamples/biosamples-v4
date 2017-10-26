package uk.ac.ebi.biosamples.solr.model.strategy;

import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.FacetQuery;
import uk.ac.ebi.biosamples.model.facet.Facet;
import uk.ac.ebi.biosamples.solr.model.field.SolrSampleField;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface FacetFetchStrategy {

    /**
     * The strategy uses the results from the facet query to return a list of optional facet
     * @param sampleRepository the repository that will be used to retrieve the facets
     * @param query the FacetQuery to retrieve the facet
     * @param facetFieldCountEntries the facet fields/count on which the facet will be calculated
     * @param pageable a page information
     * @return a list of optional facets
     */
    public List<Optional<Facet>> fetchFacetsUsing(SolrSampleRepository sampleRepository,
                                                  FacetQuery query,
                                                  List<Map.Entry<SolrSampleField, Long>> facetFieldCountEntries,
                                                  Pageable pageable);

}
