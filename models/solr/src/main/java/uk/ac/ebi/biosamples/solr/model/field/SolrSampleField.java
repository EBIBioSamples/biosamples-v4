package uk.ac.ebi.biosamples.solr.model.field;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SolrSampleField implements FilterCriteriaBuilder {

    private String readableLabel;
    private String solrDocumentLabel;


    /**
     * Constructor meant to be used only for reflection purposes
     */
    public SolrSampleField() {
        this.readableLabel = null;
        this.solrDocumentLabel = null;
    }

    /**
     * All subclasses should implement this constructor.
     * @param readableLabel
     * @param solrDocumentLabel
     */
    protected SolrSampleField(String readableLabel, String solrDocumentLabel) {
        this.readableLabel = readableLabel;
        this.solrDocumentLabel = solrDocumentLabel;
    }

    /**
     * Check if the provided string matches the field regularExpression
     * @param fieldName string to check against the field pattern
     * @return
     */
    public boolean matches(String fieldName) {
        return getFieldPattern().asPredicate().test(fieldName);
    }

    public abstract Pattern getFieldPattern();

    public abstract boolean isEncodedField();

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
