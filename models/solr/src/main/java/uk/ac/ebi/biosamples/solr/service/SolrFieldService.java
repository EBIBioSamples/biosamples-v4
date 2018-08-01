package uk.ac.ebi.biosamples.solr.service;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.io.BaseEncoding;

import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.field.SolrFieldType;
import uk.ac.ebi.biosamples.solr.model.field.SolrSampleField;

/**
 * SolrFieldService is the service that should be able to deal with all field matters
 * - Encode and decode of a field is the main reason behind it
 */
@Service
public class SolrFieldService {

//    private Logger log = LoggerFactory.getLogger(getClass());
    private List<SolrSampleField> solrFieldList;

    @Autowired
    public SolrFieldService(List<SolrSampleField> solrSampleFields) {
        this.solrFieldList = solrSampleFields;
    }

    public List<SolrSampleField> getSolrFieldList() {
        return solrFieldList;
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
                try {

                    return getNewFieldInstance(
                            fieldCandidate.getClass(),
                            baseLabel,
                            encodedField);

                } catch (NoSuchMethodException| IllegalAccessException| InvocationTargetException| InstantiationException e) {
                    throw new RuntimeException("An error occurred while instantiating creating a new instance of class " + fieldCandidate.getClass());
                }
            }
        }

        throw new RuntimeException("Provide field " + encodedField + " is unknown");
    }

    public SolrSampleField getCompatibleField(Filter filter) {

        Optional<SolrSampleField> optionalType = solrFieldList.stream()
                .filter(solrField -> solrField.isCompatibleWith(filter))
                .findFirst();
        if (optionalType.isPresent()) {
            SolrSampleField fieldCandidate = optionalType.get();
            //TODO implement methods to extract suffix and generate also the encoded label

            try {

                return getNewFieldInstance(
                        fieldCandidate.getClass(),
                        filter.getLabel());


            } catch (NoSuchMethodException| IllegalAccessException| InvocationTargetException| InstantiationException e) {
                throw new RuntimeException("An error occurred while instantiating creating a new instance of class " + fieldCandidate.getClass());
            }
        }

        throw new RuntimeException("Provide filter " + filter + " is unknown");

    }

    public SolrSampleField getNewFieldInstance(Class<? extends SolrSampleField> prototype, String baseLabel, String encodedLabel) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return prototype.getConstructor(String.class, String.class).newInstance(baseLabel, encodedLabel);
    }

    public SolrSampleField getNewFieldInstance(Class<? extends SolrSampleField> prototype, String baseLabel) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return prototype.getConstructor(String.class).newInstance(baseLabel);
    }

}

