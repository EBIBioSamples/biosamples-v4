package uk.ac.ebi.biosamples.solr.model.strategy;

import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.FacetQuery;
import uk.ac.ebi.biosamples.model.facets.Facet;
import uk.ac.ebi.biosamples.solr.model.field.SolrSampleField;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface FacetFetchStrategy {

    public List<Optional<Facet>> fetchFacetsUsing(SolrSampleRepository sampleRepository,
                                      FacetQuery query,
                                      List<Map.Entry<SolrSampleField, Long>> facetFieldCountEntries,
                                      Pageable pageable);

}
