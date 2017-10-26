package uk.ac.ebi.biosamples.solr.model.field;

import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;
import uk.ac.ebi.biosamples.solr.model.strategy.RegularFacetFetchStrategy;

public class SolrSampleInverseRelationField extends SolrSampleField {
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
    public SolrFieldType getSolrFieldType() {
        return SolrFieldType.INVERSE_RELATION;
    }

    @Override
    public FacetFetchStrategy getFacetCollectionStrategy() {
        return new RegularFacetFetchStrategy();
    }
}
