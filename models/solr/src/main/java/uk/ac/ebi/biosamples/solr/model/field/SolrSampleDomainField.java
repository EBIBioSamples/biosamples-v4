package uk.ac.ebi.biosamples.solr.model.field;

import org.springframework.data.solr.core.query.Criteria;

import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.model.filter.DomainFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;

import java.util.regex.Pattern;

@Component
public class SolrSampleDomainField extends SolrSampleField {

    public SolrSampleDomainField() {
        super();
    }

    public SolrSampleDomainField(String readableLabel) {
        super(readableLabel);
    }

    /**
     * All subclasses should implement this constructor
     *
     * @param readableLabel
     * @param solrDocumentLabel
     */
    public SolrSampleDomainField(String readableLabel, String solrDocumentLabel) {
        super(readableLabel, solrDocumentLabel);
    }

    @Override
    public Pattern getSolrFieldPattern() {
        return Pattern.compile("^(?<fieldname>domain)(?<fieldsuffix>"+getSolrFieldSuffix()+")$");
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
        return filter instanceof DomainFilter;
    }

    @Override
    public SolrFieldType getSolrFieldType() {
        return SolrFieldType.DOMAIN;
    }

    @Override
    public FacetFetchStrategy getFacetCollectionStrategy() {
        return null;
    }

    @Override
    public Criteria getFilterCriteria(Filter filter) {

        Criteria filterCriteria = null;

        if (filter instanceof DomainFilter) {

            filterCriteria = new Criteria(getSolrLabel());

            DomainFilter domainFilter = (DomainFilter) filter;
            if (domainFilter.getContent().isPresent())
//                filterCriteria = filterCriteria.expression("/" + domainFilter.getContent().get() + "/");
                filterCriteria = filterCriteria.expression(String.format("\"%s\"",domainFilter.getContent().get()));
            else
                filterCriteria = filterCriteria.isNotNull();

        }

        return filterCriteria;

    }
}
