package uk.ac.ebi.biosamples.solr.model.field;

import org.springframework.data.solr.core.query.Criteria;

import uk.ac.ebi.biosamples.model.filter.ExternalReferenceDataFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;
import uk.ac.ebi.biosamples.solr.model.strategy.RegularFacetFetchStrategy;

import java.util.regex.Pattern;

public class SolrSampleExternalReferenceDataField extends SolrSampleField{

    public SolrSampleExternalReferenceDataField() {
        super();
    }

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
    public Pattern getFieldPattern() {
        return Pattern.compile("^[A-Z0-9_]+_erd_ss$");
    }

    @Override
    public boolean isEncodedField() {
        return true;
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
//                filterCriteria = filterCriteria.expression("/" + extRefFilter.getContent().get() + "/");
                filterCriteria = filterCriteria.expression(String.format("\"%s\"",extRefFilter.getContent().get()));
            } else {
                filterCriteria = filterCriteria.isNotNull();
            }

        }

        return filterCriteria;
    }
}
