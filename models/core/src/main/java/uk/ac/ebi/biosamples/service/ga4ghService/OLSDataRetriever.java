package uk.ac.ebi.biosamples.service.ga4ghService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * OLSDAtaRetriever is api class for working with EBI OLS. It fetchs specific (see methods of class) metadata for phenopackets.
 *
 * @author Dilshat Salikhov
 */
@Service
public class OLSDataRetriever {
    private JsonNode node;

    /**
     * Read json from OLS by iri provided in GA4GH sample
     * @param iri
     */
    public void readOntologyJsonFromUrl(String iri) {
        String linkToTerm = null;
        try {
            //TODO move to application properties
            linkToTerm = "https://www.ebi.ac.uk/ols/api/terms?iri=" + URLEncoder.encode(iri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        readJson(linkToTerm);
    }

    /**
     * Read json from OLS by ontology id, for example 'efo'
     * @param id
     */
    public void readResourceInfoFromUrl(String id) {
        String linkToResourceInfo = null;
        //TODO move to application properties
        try {
            linkToResourceInfo = "https://www.ebi.ac.uk/ols/api/ontologies/" + URLEncoder.encode(id.toLowerCase(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        readJson(linkToResourceInfo);
    }

    /**
     * Fetchs json from given url and store it as Json tree in node attribute
     * @param link link to OLS element
     */
    private void readJson(String link) {
        URL urlToTerm;
        try {
            urlToTerm = new URL(link);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.node = mapper.readTree(urlToTerm);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getOntologyTermId() {
        JsonNode terms = node.get("_embedded").get("terms");
        terms = terms.get(0);
        return terms.get("obo_id").asText();
    }

    public String getOntologyTermLabel() {
        JsonNode terms = node.get("_embedded").get("terms");
        terms = terms.get(0);
        return terms.get("label").asText();
    }

    public String getResourceId() {
        return node.get("ontologyId").asText();
    }

    public String getResourceName() {
        JsonNode config = node.get("config");
        return config.get("title").asText();
    }

    public String getResourcePrefix() {
        JsonNode config = node.get("config");
        return config.get("preferredPrefix").asText();
    }

    public String getResourceUrl() {
        JsonNode config = node.get("config");
        return config.get("fileLocation").asText();
    }

    public String getResourceVersion() {
        JsonNode config = node.get("config");
        return config.get("version").asText();
    }
}
