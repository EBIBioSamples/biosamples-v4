package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.SortedSet;

@Service
public class HttpOlsUrlResolutionService {
    public static final String OLS_PREFIX = "http://www.ebi.ac.uk/ols/terms?iri=";
    public static final String OBO = "purl.obolibrary.org/obo";
    public static final String EBIUK = "www.ebi.ac.uk";
    public static Logger log = LoggerFactory.getLogger(HttpOlsUrlResolutionService.class);

    public HttpOlsUrlResolutionService() {
    }

    /*
    --To check if the URL resolves--
    --We are not using this at this moment--
    public boolean checkHttpStatusOfUrl(final String urlToCheck) throws IOException {
        final URL url = new URL(urlToCheck);
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        int response;

        conn.setRequestMethod("GET");
        conn.connect();
        response = conn.getResponseCode();

        return HttpStatus.valueOf(response).is2xxSuccessful();
    }*/

    /**
     * This returns a string representation of the URL to lookup the associated ontology term iri in
     * EBI OLS.
     *
     * @return url representation of the IRI
     */
    //@Cacheable(value = "iri")
    public String getIriOls(final SortedSet<String> iri) {
        if (iri == null || iri.size() == 0) return null;

        String displayIri = iri.first();

        //check this is a sane iri
        try {
            final UriComponents iriComponents = UriComponentsBuilder.fromUriString(displayIri).build(true);

            if (iriComponents.getScheme() == null
                    || iriComponents.getHost() == null
                    || iriComponents.getPath() == null) {
                //incomplete iri (e.g. 9606, EFO_12345) don't bother to check
                return null;
            }
        } catch (final Exception e) {
            //FIXME: Can't use a non static logger here because
            log.error("An error occurred while trying to build OLS iri for " + displayIri, e);
            return null;
        }

        //TODO application.properties this
        //TODO use https
        final String iriUrl = URLEncoder.encode(displayIri, StandardCharsets.UTF_8);

        if (checkUrlForPattern(displayIri)) {
            return OLS_PREFIX + iriUrl;
        } else {
            return displayIri;
        }
    }

    public boolean checkUrlForPattern(final String displayIri) {
        if (displayIri.contains(OBO) || displayIri.contains(EBIUK))
            return true;
        else return false;
    }
}
