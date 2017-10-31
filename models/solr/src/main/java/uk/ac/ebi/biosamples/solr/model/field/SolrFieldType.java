package uk.ac.ebi.biosamples.solr.model.field;

import uk.ac.ebi.biosamples.model.FacetFilterFieldType;
import uk.ac.ebi.biosamples.model.filter.FilterType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.Optional;

import static uk.ac.ebi.biosamples.solr.model.field.SolrFieldType.FieldEncodingType.ENCODED;
import static uk.ac.ebi.biosamples.solr.model.field.SolrFieldType.FieldEncodingType.NOT_ENCODED;


/**
 * Enum representing the different type of fields available in the Solr document. These fields are linked to
 * the FacetFilterFieldType so that is possible to retrieve the corresponding Facet and Filters
 *
 */
public enum SolrFieldType {
    ATTRIBUTE(FacetFilterFieldType.ATTRIBUTE, "_av_ss", ENCODED, SolrSampleAttributeValueField.class),
    RELATION(FacetFilterFieldType.RELATION, "_or_ss", ENCODED, SolrSampleRelationField.class),
    INVERSE_RELATION(FacetFilterFieldType.INVERSE_RELATION, "_ir_ss", ENCODED, SolrSampleInverseRelationField.class),
    DATE(FacetFilterFieldType.UPDATE_DATE, "_dt", NOT_ENCODED, SolrSampleDateField.class),
    DOMAIN(FacetFilterFieldType.DOMAIN, "_s", NOT_ENCODED, SolrSampleDomainField.class),
    EXTERNAL_REFERENCE_DATA(FacetFilterFieldType.EXTERNAL_REFERENCE_DATA, "_erd_ss", ENCODED, SolrSampleExternalReferenceDataField.class),
    NAME(FacetFilterFieldType.NAME, "_s", NOT_ENCODED, SolrSampleNameField.class),
    ACCESSION(FacetFilterFieldType.ACCESSION, "", NOT_ENCODED, SolrSampleAccessionField.class);


    private static EnumMap<FilterType, SolrFieldType> filterToSolrFieldMap = new EnumMap<FilterType, SolrFieldType>(FilterType.class);

    static {
        for(SolrFieldType fieldType: values()) {
            Optional<FilterType> filterAssociatedWithField = fieldType.getFacetFilterFieldType().getFilterType();
            filterAssociatedWithField.ifPresent(filterType -> filterToSolrFieldMap.put(filterType, fieldType));
        }
    }



    private final FacetFilterFieldType facetFilterFieldType;
    private final String suffix;
    private final FieldEncodingType isEncoded;
    private final Class<? extends SolrSampleField> associatedClass;

    SolrFieldType(FacetFilterFieldType facetFilterFieldType,
                  String suffix,
                  FieldEncodingType encoding,
                  Class<? extends SolrSampleField> associatedClass) {
        this.facetFilterFieldType = facetFilterFieldType;
        this.suffix = suffix;
        this.isEncoded = encoding;
        this.associatedClass = associatedClass;
    }

    public FacetFilterFieldType getFacetFilterFieldType() {
        return facetFilterFieldType;
    }

    public String getSuffix() {
        return suffix;
    }

    public Boolean isEncoded() {
        return isEncoded.equals(ENCODED);
    }

    public SolrSampleField getAssociatedClassInstance(String label, String solrDocumentLabel) {
        try {
            Constructor<? extends SolrSampleField> constructor = this.associatedClass.getConstructor(String.class, String.class);
            return constructor.newInstance(label, solrDocumentLabel);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }


    public static SolrFieldType getFromField(String field) {
        for(SolrFieldType type: values()) {
            if (field.endsWith(type.getSuffix())) {
                return type;
            }
        }

        throw new RuntimeException("Provide field " + field + " is unknown");
    }

    public static SolrFieldType getFromFilterType(FilterType filterType) {
        SolrFieldType fieldType = filterToSolrFieldMap.get(filterType);
        if (fieldType == null) {
            throw new RuntimeException("Provide filter type " + filterType + " is not associated with a field");
        }
        return fieldType;
    }

    public enum FieldEncodingType {
        ENCODED, NOT_ENCODED;
    }
}

