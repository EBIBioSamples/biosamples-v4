package uk.ac.ebi.biosamples.solr.model.field;

import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;

public abstract class SolrSampleField {

    private String readableLabel;
    private String solrDocumentLabel;

    /**
     * All subclasses should implement this constructor
     * @param readableLabel
     * @param solrDocumentLabel
     */
    protected SolrSampleField(String readableLabel, String solrDocumentLabel) {
        this.readableLabel = readableLabel;
        if(this.getSolrFieldType().isEncoded()) {
            this.solrDocumentLabel = solrDocumentLabel;
        } else {
            this.solrDocumentLabel = readableLabel;
        }
    }

    public abstract SolrFieldType getSolrFieldType();
    public abstract FacetFetchStrategy getFacetCollectionStrategy();


    public String getLabel() {
        return this.readableLabel;
    }

    public String getSolrDocumentFieldName() {
        return solrDocumentLabel;
    }




}
