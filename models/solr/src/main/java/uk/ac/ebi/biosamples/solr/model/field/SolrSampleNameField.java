package uk.ac.ebi.biosamples.solr.model.field;

import org.springframework.data.solr.core.query.Criteria;

import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.model.facet.Facet;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.model.filter.NameFilter;
import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class SolrSampleNameField extends SolrSampleField {

    public SolrSampleNameField() {
        super();
    }

    public SolrSampleNameField(String readableLabel) {
        super(readableLabel);
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
    public Pattern getSolrFieldPattern() {
        return Pattern.compile("^(?<fieldname>name)(?<fieldsuffix>"+getSolrFieldSuffix()+")$");
    }

    @Override
    public String getSolrFieldSuffix() {
        return "_s";
    }

    @Override
    public boolean isEncodedField() {
        return false;
    }

    @Override
    public boolean isCompatibleWith(Filter filter) {
        return filter instanceof NameFilter;
    }

    @Override
    public FacetFetchStrategy getFacetCollectionStrategy() {
        return null;
    }

    @Override
    public Criteria getFilterCriteria(Filter filter) {
        Criteria filterCriteria = null;

        if (filter instanceof NameFilter) {

            filterCriteria = new Criteria(getSolrLabel());

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
