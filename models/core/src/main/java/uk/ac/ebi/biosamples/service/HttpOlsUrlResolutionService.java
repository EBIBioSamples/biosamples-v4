package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.SortedSet;

@Service
public class HttpOlsUrlResolutionService {
    public static Logger log = LoggerFactory.getLogger(HttpOlsUrlResolutionService.class);

    public HttpOlsUrlResolutionService() {
    }

    public boolean checkHttpStatusOfUrl(final String urlToCheck) throws IOException {
        URL url = new URL(urlToCheck);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        int response;

        conn.setRequestMethod("GET");
        conn.connect();
        response = conn.getResponseCode();

        log.info("Status is " +response+ " for URL " + urlToCheck);

        return HttpStatus.valueOf(response).is2xxSuccessful();
    }

    /**
     * This returns a string representation of the URL to lookup the associated ontology term iri in
     * EBI OLS.
     *
     * @return url representation of the IRI
     */
    public String getIriOls(SortedSet<String> iri) {
        if (iri == null || iri.size() == 0) return null;

        String displayIri = iri.first();

        //check this is a sane iri
        try {
            UriComponents iriComponents = UriComponentsBuilder.fromUriString(displayIri).build(true);
            if (iriComponents.getScheme() == null
                    || iriComponents.getHost() == null
                    || iriComponents.getPath() == null) {
                //incomplete iri (e.g. 9606, EFO_12345) don't bother to check
                return null;
            }
        } catch (Exception e) {
            //FIXME: Can't use a non static logger here because
            log.error("An error occurred while trying to build OLS iri for " + displayIri, e);
            return null;
        }

        //TODO application.properties this
        //TODO use https
        final String iriUrl = URLEncoder.encode(displayIri, StandardCharsets.UTF_8);

        try {
            if(checkUrlForPattern(displayIri)) {
                //log.info("in p1 " + displayIri);
                if (checkHttpStatusOfUrl("http://www.ebi.ac.uk/ols/terms?iri=" + displayIri)) {
                    //log.info("in p2 " + displayIri);
                    return "http://www.ebi.ac.uk/ols/terms?iri=" + iriUrl;
                } else {
                    return displayIri;
                }
            } else {
                return "http://www.ebi.ac.uk/ols/terms?iri=" + iriUrl;
            }
        } catch (IOException e) {
            return displayIri;
        }
    }

    public boolean checkUrlForPattern(String displayIri) {
        //log.info(displayIri);

        if(displayIri.contains("purl.obolibrary.org/obo") || displayIri.contains("www.ebi.ac.uk/efo/EFO")) {
            //log.info(displayIri + "  " + false);
            return false;
        }

        else return true;
    }
}
