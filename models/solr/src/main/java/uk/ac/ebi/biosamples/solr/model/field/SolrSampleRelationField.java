package uk.ac.ebi.biosamples.solr.model.field;

import org.springframework.data.solr.core.query.Criteria;

import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.model.facet.Facet;
import uk.ac.ebi.biosamples.model.facet.RelationFacet;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.model.filter.RelationFilter;
import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;
import uk.ac.ebi.biosamples.solr.model.strategy.RegularFacetFetchStrategy;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class SolrSampleRelationField extends SolrSampleField{

    public SolrSampleRelationField() {
        super();
    }

    public SolrSampleRelationField(String readableLabel) {
        super(readableLabel);
    }

    /**
     * All subclasses should implement this constructor
     *
     * @param readableLabel
     * @param solrDocumentLabel
     */
    public SolrSampleRelationField(String readableLabel, String solrDocumentLabel) {
        super(readableLabel, solrDocumentLabel);
    }

    @Override
    public Pattern getSolrFieldPattern() {
        return Pattern.compile("^(?<fieldname>[A-Z0-9_]+)(?<fieldsuffix>"+getSolrFieldSuffix()+")$");
    }

    @Override
    public String getSolrFieldSuffix() {
        return "_or_ss";
    }

    @Override
    public boolean isEncodedField() {
        return true;
    }

    @Override
    public boolean isCompatibleWith(Filter filter) {
        return filter instanceof RelationFilter;
    }

    @Override
    public boolean canGenerateFacets() {
        return true;
    }


    @Override
    public Facet.Builder getFacetBuilder(String facetLabel, Long facetCount) {
        return new RelationFacet.Builder(facetLabel, facetCount);
    }

    @Override
    public SolrFieldType getSolrFieldType() {
        return SolrFieldType.RELATION;
    }

    @Override
    public FacetFetchStrategy getFacetCollectionStrategy() {
        return new RegularFacetFetchStrategy();
    }

    @Override
    public Criteria getFilterCriteria(Filter filter) {
        Criteria filterCriteria = null;

        if (filter instanceof RelationFilter) {

            filterCriteria = new Criteria(getSolrLabel());

            RelationFilter relationFilter = (RelationFilter) filter;
            if (relationFilter.getContent().isPresent()) {
//                filterCriteria = filterCriteria.expression("/" + relationFilter.getContent().get() + "/");
                filterCriteria = filterCriteria.expression(String.format("\"%s\"", relationFilter.getContent().get()));
            } else {
                filterCriteria = filterCriteria.isNotNull();
            }
        }

        return filterCriteria;
    }
}
