package uk.ac.ebi.biosamples.solr.model.field;

import org.springframework.data.solr.core.query.Criteria;
import uk.ac.ebi.biosamples.model.filter.DataTypeFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.model.filter.NameFilter;
import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;
import uk.ac.ebi.biosamples.solr.model.strategy.RegularFacetFetchStrategy;

import java.util.regex.Pattern;

public class SolrSampleDataTypeField extends SolrSampleField {

    public SolrSampleDataTypeField() {
        super();
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
    public Pattern getFieldPattern() {
        return Pattern.compile("^structdatatype_ss$");
    }

    @Override
    public boolean isEncodedField() {
        return false;
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

            filterCriteria = new Criteria(getSolrDocumentFieldName());

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
