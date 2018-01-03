package uk.ac.ebi.biosamples.solr.model.field;

import org.springframework.data.solr.core.query.Criteria;

import uk.ac.ebi.biosamples.model.filter.DomainFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;

public class SolrSampleDomainField extends SolrSampleField {
    /**
     * All subclasses should implement this constructor
     *
     * @param readableLabel
     * @param solrDocumentLabel
     */
    public SolrSampleDomainField(String readableLabel, String solrDocumentLabel) {
        super(readableLabel, solrDocumentLabel);
    }

    @Override
    public SolrFieldType getSolrFieldType() {
        return SolrFieldType.DOMAIN;
    }

    @Override
    public FacetFetchStrategy getFacetCollectionStrategy() {
        return null;
    }

    @Override
    public Criteria getFilterCriteria(Filter filter) {

        Criteria filterCriteria = null;

        if (filter instanceof DomainFilter) {

            filterCriteria = new Criteria(getSolrDocumentFieldName());

            DomainFilter domainFilter = (DomainFilter) filter;
            if (domainFilter.getContent().isPresent())
                filterCriteria = filterCriteria.expression("/" + domainFilter.getContent().get() + "/");
            else
                filterCriteria = filterCriteria.isNotNull();

        }

        return filterCriteria;

    }
}
