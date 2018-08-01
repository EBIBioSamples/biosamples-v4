package uk.ac.ebi.biosamples.solr.model.field;

import org.springframework.data.solr.core.query.Criteria;

import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.model.filter.AccessionFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;

import java.util.regex.Pattern;

@Component
public class SolrSampleAccessionField extends SolrSampleField {


    public SolrSampleAccessionField() {
        super();
    }

    public SolrSampleAccessionField(String readableLabel) {
        super(readableLabel);
    }

    /**
     * All subclasses should implement this constructor
     *
     * @param readableLabel
     * @param solrDocumentLabel
     */
    public SolrSampleAccessionField(String readableLabel, String solrDocumentLabel) {
        super(readableLabel, solrDocumentLabel);
    }

    @Override
    public Pattern getFieldPattern() {
        return Pattern.compile("^(?<fieldname>id)$");
    }

    @Override
    public boolean isEncodedField() {
        return false;
    }

    @Override
    public String getSolrFieldSuffix() {
        return "";
    }

    @Override
    public boolean isCompatibleWith(Filter filter) {
        return filter instanceof AccessionFilter;
    }

    @Override
    public SolrFieldType getSolrFieldType() {
        return SolrFieldType.ACCESSION;
    }

    @Override
    public FacetFetchStrategy getFacetCollectionStrategy() {
        return null;
    }

    @Override
    public Criteria getFilterCriteria(Filter filter) {

        Criteria filterCriteria = null;

        if (filter instanceof AccessionFilter) {

            filterCriteria = new Criteria(getSolrLabel());

            AccessionFilter accessionFilter = (AccessionFilter) filter;
            if (accessionFilter.getContent().isPresent())
                filterCriteria = filterCriteria.expression(String.format("/%s/",accessionFilter.getContent().get()));
            else
                filterCriteria = filterCriteria.isNotNull();
        }

        return filterCriteria;

    }
}
