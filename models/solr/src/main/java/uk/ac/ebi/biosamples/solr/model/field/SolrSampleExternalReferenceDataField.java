package uk.ac.ebi.biosamples.solr.model.field;

import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;
import uk.ac.ebi.biosamples.solr.model.strategy.RegularFacetFetchStrategy;

public class SolrSampleExternalReferenceDataField extends SolrSampleField{

    /**
     * All subclasses should implement this constructor.
     *
     * @param readableLabel
     * @param solrDocumentLabel
     */
    public SolrSampleExternalReferenceDataField(String readableLabel, String solrDocumentLabel) {
        super(readableLabel, solrDocumentLabel);
    }

    @Override
    public SolrFieldType getSolrFieldType() {
        return SolrFieldType.EXTERNAL_REFERENCE_DATA;
    }

    @Override
    public FacetFetchStrategy getFacetCollectionStrategy() {
        return new RegularFacetFetchStrategy();
    }
}
