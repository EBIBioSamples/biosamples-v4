package uk.ac.ebi.biosamples.solr.model.field;

import org.springframework.data.solr.core.query.Criteria;

import uk.ac.ebi.biosamples.model.filter.AttributeFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;
import uk.ac.ebi.biosamples.solr.model.strategy.RegularFacetFetchStrategy;
import uk.ac.ebi.biosamples.solr.service.SolrFieldService;

import java.util.regex.Pattern;

public class SolrSampleAttributeValueField extends SolrSampleField {

    public SolrSampleAttributeValueField() {
        super();
    }

    public SolrSampleAttributeValueField(String label, String documentField) {
        super(label, documentField);
    }

    @Override
    public boolean isEncodedField() {
        return true;
    }

    @Override
    public Pattern getFieldPattern() {
        return Pattern.compile("^[A-Z0-9_]+_av_ss$");
    }

    @Override
    public FacetFetchStrategy getFacetCollectionStrategy() {
        return new RegularFacetFetchStrategy();
    }

    @Override
    public SolrFieldType getSolrFieldType() {
        return SolrFieldType.ATTRIBUTE;
    }

    @Override
    public Criteria getFilterCriteria(Filter filter) {


        Criteria filterCriteria = null;
        if (filter instanceof AttributeFilter) {

            filterCriteria = new Criteria(this.getSolrDocumentFieldName());

            AttributeFilter attributeFilter = (AttributeFilter) filter;
            if (attributeFilter.getContent().isPresent())
//                filterCriteria.expression(String.format("/%s/",attributeFilter.getContent().get()));
                filterCriteria = filterCriteria.expression(String.format("\"%s\"",attributeFilter.getContent().get()));
            else
                filterCriteria = filterCriteria.isNotNull();

        }

        return filterCriteria;
    }
}
