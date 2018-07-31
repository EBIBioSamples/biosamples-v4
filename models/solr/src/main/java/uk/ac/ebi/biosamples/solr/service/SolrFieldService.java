package uk.ac.ebi.biosamples.solr.service;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.io.BaseEncoding;

import uk.ac.ebi.biosamples.solr.model.field.SolrFieldType;
import uk.ac.ebi.biosamples.solr.model.field.SolrSampleField;

/**
 * SolrFieldService is the service that should be able to deal with all field matters
 * - Encode and decode of a field is the main reason behind it
 */
@Service
public class SolrFieldService {

//    private Logger log = LoggerFactory.getLogger(getClass());
    List<SolrSampleField> solrFieldList;

    @Autowired
    public SolrFieldService(List<SolrSampleField> solrSampleFields) {
        this.solrFieldList = solrSampleFields;
    }

    public static String encodeFieldName(String field) {
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

    public static String decodeFieldName(String encodedField) {
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
    public static String encodedField(String field, SolrFieldType solrFieldType) {

        // Dates fields (update and release) are not encoded at the moment
        if (solrFieldType.isEncoded()) {
            return encodeFieldName(field) + solrFieldType.getSuffix();
        }
        return field + solrFieldType.getSuffix();
    }

    /**
     * Try to decode a field guessing the facet type
     *
     * @param encodedField encoded version of the field with the type suffix
     * @return the field name decoded
     */
    public SolrSampleField decodeField(String encodedField) {

        Optional<SolrSampleField> optionalType = solrFieldList.stream()
                .filter(solrField -> solrField.matches(encodedField))
                .findFirst();
        if (optionalType.isPresent()) {
            SolrSampleField fieldCandidate = optionalType.get();
            Matcher m = fieldCandidate.getFieldPattern().matcher(encodedField);
            if (m.find()) {
                String baseLabel = m.group("fieldname");

                if (fieldCandidate.isEncodedField()) {
                    baseLabel = decodeFieldName(baseLabel);
                }

                fieldCandidate.setReadableLabel(baseLabel);
                fieldCandidate.setSolrLabel(encodedField);

                return fieldCandidate;
            }

        }

        throw new RuntimeException("Provide field " + encodedField + " is unknown");
    }

}

