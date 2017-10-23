package uk.ac.ebi.biosamples.solr.service;

import com.google.common.io.BaseEncoding;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.solr.model.field.SolrFieldType;
import uk.ac.ebi.biosamples.solr.model.field.SolrSampleField;

import java.io.UnsupportedEncodingException;

/**
 * SolrFieldService is the service that should be able to deal with all field matters
 * - Encode and decode of a field is the main reason behind it
 */
@Service
public class SolrFieldService {

//    private Logger log = LoggerFactory.getLogger(getClass());

    public String encodeFieldName(String field) {
        //solr only allows alphanumeric field types
        try {
            field = BaseEncoding.base32().encode(field.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        //although its base32 encoded, that include = which solr doesn't allow
        field = field.replaceAll("=", "_");

        return field;
    }

    public String decodeFieldName(String encodedField) {
        //although its base32 encoded, that include = which solr doesn't allow
        String decodedField = encodedField.replace("_", "=");
        try {
            decodedField = new String(BaseEncoding.base32().decode(decodedField), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return decodedField;
    }

    /**
     * Provide the encoded name of the field using a specific facet type
     * @param field the regular field name
     * @param solrFieldType the facet type
     * @return the encoded field with specific type suffix
     */
    public String encodedField(String field, SolrFieldType solrFieldType) {

        // Dates fields (update and release) are not encoded at the moment
        if (solrFieldType.isEncoded()) {
            return this.encodeFieldName(field) + solrFieldType.getSuffix();
        }
        return field + solrFieldType.getSuffix();
    }

    /**
     * Try to decode a field guessing the facet type
     *
     * @param field encoded version of the field with the type suffix
     * @return the field name decoded
     */
    public SolrSampleField decodeField(String field) {

        SolrFieldType fieldType = SolrFieldType.getFromField(field);
        String baseLabel = field.replaceFirst(
                fieldType.getSuffix() + "$",
                "");
        if (fieldType.isEncoded()) {
           baseLabel = decodeFieldName(baseLabel);
        }
        return fieldType.getAssociatedClassInstance(baseLabel, field);
    }

}

