package uk.ac.ebi.biosamples.solr.model.field;

import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;

public class SolrSampleNameField extends SolrSampleField {
    /**
     * All subclasses should implement this constructor
     *
     * @param readableLabel
     * @param solrDocumentLabel
     */
    protected SolrSampleNameField(String readableLabel, String solrDocumentLabel) {
        super(readableLabel, solrDocumentLabel);
    }

    @Override
    public SolrFieldType getSolrFieldType() {
        return SolrFieldType.NAME;
    }

    @Override
    public FacetFetchStrategy getFacetCollectionStrategy() {
        return null;
    }
}
