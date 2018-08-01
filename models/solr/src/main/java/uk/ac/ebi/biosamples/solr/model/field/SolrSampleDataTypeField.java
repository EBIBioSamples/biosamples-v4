package uk.ac.ebi.biosamples.solr.model.field;

import org.springframework.data.solr.core.query.Criteria;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.model.facet.DataTypeFacet;
import uk.ac.ebi.biosamples.model.facet.Facet;
import uk.ac.ebi.biosamples.model.filter.DataTypeFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.model.structured.DataType;
import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;
import uk.ac.ebi.biosamples.solr.model.strategy.RegularFacetFetchStrategy;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class SolrSampleDataTypeField extends SolrSampleField {

    public SolrSampleDataTypeField() {
        super();
    }

    public SolrSampleDataTypeField(String readableLabel) {
        super(readableLabel);
    }

    /**
     * All subclasses should implement this constructor
     *
     * @param readableLabel
     * @param solrDocumentLabel
     */
    public SolrSampleDataTypeField(String readableLabel, String solrDocumentLabel) {
        super(readableLabel, solrDocumentLabel);
    }

    @Override
    public Pattern getSolrFieldPattern() {
        return Pattern.compile("^(?<fieldname>structdatatype)(?<fieldsuffix>" + getSolrFieldSuffix() + ")$");
    }

    @Override
    public String getSolrFieldSuffix() {
        return "_ss";
    }

    @Override
    public boolean isEncodedField() {
        return false;
    }

    @Override
    public boolean isCompatibleWith(Filter filter) {
        return filter instanceof DataTypeFilter;
    }

    @Override
    public boolean canGenerateFacets() {
        return true;
    }

    @Override
    public Facet.Builder getFacetBuilder(String facetLabel, Long facetCount) {
        return new DataTypeFacet.Builder("Contains data", facetCount);
    }

    @Override
    public SolrFieldType getSolrFieldType() {
        return SolrFieldType.STRUCTURED_DATA_TYPES;
    }

    @Override
    public FacetFetchStrategy getFacetCollectionStrategy() {
        return new RegularFacetFetchStrategy();
    }

    @Override
    public Criteria getFilterCriteria(Filter filter) {
        Criteria filterCriteria = null;

        if (filter instanceof DataTypeFilter) {

            filterCriteria = new Criteria(getSolrLabel());

            DataTypeFilter nameFilter = (DataTypeFilter) filter;
            if (nameFilter.getContent().isPresent()) {
//                filterCriteria = filterCriteria.expression("/" + nameFilter.getContent().get() + "/");
                filterCriteria = filterCriteria.expression(String.format("\"%s\"", nameFilter.getContent().get()));
            } else {
                filterCriteria = filterCriteria.isNotNull();
            }
        }

        return filterCriteria;
    }
}
