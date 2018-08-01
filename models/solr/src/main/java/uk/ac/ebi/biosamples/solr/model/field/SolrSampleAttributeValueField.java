package uk.ac.ebi.biosamples.solr.model.field;

import org.springframework.data.solr.core.query.Criteria;

import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.model.facet.AttributeFacet;
import uk.ac.ebi.biosamples.model.facet.Facet;
import uk.ac.ebi.biosamples.model.filter.AttributeFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;
import uk.ac.ebi.biosamples.solr.model.strategy.RegularFacetFetchStrategy;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class SolrSampleAttributeValueField extends SolrSampleField {

    public SolrSampleAttributeValueField() {
        super();
    }

    public SolrSampleAttributeValueField(String readableLabel) {
        super(readableLabel);
    }

    public SolrSampleAttributeValueField(String label, String documentField) {
        super(label, documentField);
    }

    @Override
    public boolean isEncodedField() {
        return true;
    }

    @Override
    public Pattern getSolrFieldPattern() {
        return Pattern.compile("^(?<fieldname>[A-Z0-9_]+)(?<fieldsuffix>" + getSolrFieldSuffix() + ")$");
    }

    @Override
    public String getSolrFieldSuffix() {
        return "_av_ss";
    }

    @Override
    public boolean canGenerateFacets() {
        return true;
    }

    @Override
    public Facet.Builder getFacetBuilder(String facetLabel, Long facetCount) {
        return new AttributeFacet.Builder(facetLabel, facetCount);
    }

    @Override
    public FacetFetchStrategy getFacetCollectionStrategy() {
        return new RegularFacetFetchStrategy();
    }

    @Override
    public boolean isCompatibleWith(Filter filter) {
        return filter instanceof AttributeFilter;
    }

    @Override
    public SolrFieldType getSolrFieldType() {
        return SolrFieldType.ATTRIBUTE;
    }

    @Override
    public Criteria getFilterCriteria(Filter filter) {


        Criteria filterCriteria = null;
        if (filter instanceof AttributeFilter) {

            filterCriteria = new Criteria(this.getSolrLabel());

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
