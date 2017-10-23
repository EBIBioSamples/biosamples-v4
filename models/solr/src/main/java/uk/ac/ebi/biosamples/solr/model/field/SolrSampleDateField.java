package uk.ac.ebi.biosamples.solr.model.field;

import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;

public class SolrSampleDateField extends SolrSampleField{
    /**
     * All subclasses should implement this constructor
     *
     * @param readableLabel
     * @param solrDocumentLabel
     */
    public SolrSampleDateField(String readableLabel, String solrDocumentLabel) {
        super(readableLabel, solrDocumentLabel);
    }

    @Override
    public SolrFieldType getSolrFieldType() {
        return SolrFieldType.DATE;
    }

    @Override
    public FacetFetchStrategy getFacetCollectionStrategy() {
        throw new RuntimeException("Method not yet implemented");
    }

}
