package uk.ac.ebi.biosamples.solr.model.field;

import org.springframework.data.solr.core.query.Criteria;

import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.model.filter.InverseRelationFilter;
import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;
import uk.ac.ebi.biosamples.solr.model.strategy.RegularFacetFetchStrategy;

import java.util.regex.Pattern;

public class SolrSampleInverseRelationField extends SolrSampleField {

    public SolrSampleInverseRelationField() {
        super();
    }

    /**
     * All subclasses should implement this constructor
     *
     * @param readableLabel
     * @param solrDocumentLabel
     */
    public SolrSampleInverseRelationField(String readableLabel, String solrDocumentLabel) {
        super(readableLabel, solrDocumentLabel);
    }

    @Override
    public Pattern getFieldPattern() {
        return Pattern.compile("^[A-Z0-9_]+_ir_ss$");
    }

    @Override
    public boolean isEncodedField() {
        return true;
    }

    @Override
    public SolrFieldType getSolrFieldType() {
        return SolrFieldType.INVERSE_RELATION;
    }

    @Override
    public FacetFetchStrategy getFacetCollectionStrategy() {
        return new RegularFacetFetchStrategy();
    }

    @Override
    public Criteria getFilterCriteria(Filter filter) {
        Criteria filterCriteria = null;

        if (filter instanceof InverseRelationFilter) {

            filterCriteria = new Criteria(getSolrDocumentFieldName());

            InverseRelationFilter inverseRelationFilter = (InverseRelationFilter) filter;
            if (inverseRelationFilter.getContent().isPresent()) {
//                filterCriteria = filterCriteria.expression("/" + inverseRelationFilter.getContent().get() + "/");
                filterCriteria = filterCriteria.expression(String.format("\"%s\"",inverseRelationFilter.getContent().get()));
            } else {
                filterCriteria = filterCriteria.isNotNull();
            }
        }

        return filterCriteria;
    }
}
