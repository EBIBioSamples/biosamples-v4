package uk.ac.ebi.biosamples.solr.model.field;

import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;
import uk.ac.ebi.biosamples.solr.model.strategy.RegularFacetFetchStrategy;

public class SolrSampleAttributeValueField extends SolrSampleField {

    public SolrSampleAttributeValueField(String label, String documentField) {
        super(label, documentField);
    }

    @Override
    public FacetFetchStrategy getFacetCollectionStrategy() {
        return new RegularFacetFetchStrategy();
    }

    @Override
    public SolrFieldType getSolrFieldType() {
        return SolrFieldType.ATTRIBUTE;
    }
}
