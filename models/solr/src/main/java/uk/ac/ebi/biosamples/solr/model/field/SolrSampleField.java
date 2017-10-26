package uk.ac.ebi.biosamples.solr.model.field;

import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;

public abstract class SolrSampleField {

    private String readableLabel;
    private String solrDocumentLabel;

    /**
     * All subclasses should implement this constructor.
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

    /**
     * For each field a specific strategy to get the facet content need to be implemented
     * Facet content retrieve will be delegated to the facet fetch strategy
     * @return a facet fetch strategy
     */
    public abstract FacetFetchStrategy getFacetCollectionStrategy();


    /**
     * @return the readable label of the field
     */
    public String getLabel() {
        return this.readableLabel;
    }

    /**
     * @return the document field, which could be encoded or not encoded based on the SolrFieldType
     */
    public String getSolrDocumentFieldName() {
        return solrDocumentLabel;
    }




}
