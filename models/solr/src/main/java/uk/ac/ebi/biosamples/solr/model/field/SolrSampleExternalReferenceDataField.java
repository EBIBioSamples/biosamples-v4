package uk.ac.ebi.biosamples.solr.model.field;

import org.springframework.data.solr.core.query.Criteria;
import uk.ac.ebi.biosamples.model.filter.ExternalReferenceDataFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;
import uk.ac.ebi.biosamples.solr.model.strategy.RegularFacetFetchStrategy;

public class SolrSampleExternalReferenceDataField extends SolrSampleField{

    /**
     * All subclasses should implement this constructor.
     *
     * @param readableLabel
     * @param solrDocumentLabel
     */
    public SolrSampleExternalReferenceDataField(String readableLabel, String solrDocumentLabel) {
        super(readableLabel, solrDocumentLabel);
    }

    @Override
    public SolrFieldType getSolrFieldType() {
        return SolrFieldType.EXTERNAL_REFERENCE_DATA;
    }

    @Override
    public FacetFetchStrategy getFacetCollectionStrategy() {
        return new RegularFacetFetchStrategy();
    }

    @Override
    public Criteria getFilterCriteria(Filter filter) {
        Criteria filterCriteria = null;

        if (filter instanceof ExternalReferenceDataFilter) {

            filterCriteria = new Criteria(getSolrDocumentFieldName());

            ExternalReferenceDataFilter extRefFilter = (ExternalReferenceDataFilter) filter;
            if (extRefFilter.getContent().isPresent()) {
                filterCriteria = filterCriteria.expression("/" + extRefFilter.getContent().get() + "/");
            } else {
                filterCriteria = filterCriteria.isNotNull();
            }

        }

        return filterCriteria;
    }
}
