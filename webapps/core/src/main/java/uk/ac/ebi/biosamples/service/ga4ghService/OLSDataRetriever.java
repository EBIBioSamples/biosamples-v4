package uk.ac.ebi.biosamples.service.ga4ghService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class OLSDataRetriever {
    private JsonNode node;

    public void readJsonFromUrl(String iri) {
        String linkToTerm = null;
        try {
            //TODO move to application properties
            linkToTerm = "https://www.ebi.ac.uk/ols/api/terms?iri=" + URLEncoder.encode(iri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        URL urlToTerm;
        try {
            urlToTerm = new URL(linkToTerm);
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

    public String StringGetOntologyTermId() {
        JsonNode terms = node.get("_embedded").get("terms");
        terms = terms.get(0);
        return terms.get("obo_id").asText();
    }

    public String StringGetOntologyTermLabel() {
        JsonNode terms = node.get("_embedded").get("terms");
        terms = terms.get(0);
        return terms.get("label").asText();
    }

}
