package uk.ac.ebi.biosamples.curation.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Service to validate if an IRI URL resolves
 */
@Service
public class IriUrlValidatorService {
    public static final String OBO = "purl.obolibrary.org/obo";
    public static final String EBIUK = "www.ebi.ac.uk";

    public IriUrlValidatorService(){}

    //@Cacheable(value = "iri")
    public boolean validateIri(final String iri) {
        try {
            return checkHttpStatusOfUrl(iri);
        } catch (IOException e) {
            return false;
        }
    }

    public boolean checkHttpStatusOfUrl(final String urlToCheck) throws IOException {
        final URL url = new URL(urlToCheck);
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        int response;

        conn.setRequestMethod("GET");
        conn.connect();
        response = conn.getResponseCode();

        return HttpStatus.valueOf(response).is2xxSuccessful();
    }

    public boolean checkUrlForPattern(final String displayIri) {
        if (displayIri.contains(OBO) || displayIri.contains(EBIUK))
            return false;
        else return true;
    }
}
