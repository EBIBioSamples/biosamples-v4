package uk.ac.ebi.biosamples.solr.model.field;

import org.springframework.data.solr.core.query.Criteria;

import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.model.filter.NameFilter;
import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;

import java.util.regex.Pattern;

public class SolrSampleNameField extends SolrSampleField {

    public SolrSampleNameField() {
        super();
    }

    /**
     * All subclasses should implement this constructor
     *
     * @param readableLabel
     * @param solrDocumentLabel
     */
    public SolrSampleNameField(String readableLabel, String solrDocumentLabel) {
        super(readableLabel, solrDocumentLabel);
    }

    @Override
    public Pattern getFieldPattern() {
        return Pattern.compile("^name_s$");
    }

    @Override
    public boolean isEncodedField() {
        return false;
    }

    @Override
    public SolrFieldType getSolrFieldType() {
        return SolrFieldType.NAME;
    }

    @Override
    public FacetFetchStrategy getFacetCollectionStrategy() {
        return null;
    }

    @Override
    public Criteria getFilterCriteria(Filter filter) {
        Criteria filterCriteria = null;

        if (filter instanceof NameFilter) {

            filterCriteria = new Criteria(getSolrDocumentFieldName());

            NameFilter nameFilter = (NameFilter) filter;
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
