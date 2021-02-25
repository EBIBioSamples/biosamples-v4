package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.model.auth.SubmissionAccount;

@Service
public class BioSamplesWebinAuthenticationService {
    private Logger log = LoggerFactory.getLogger(getClass());

    private final RestTemplate restTemplate;
    //private final BioSamplesProperties bioSamplesProperties;

    public BioSamplesWebinAuthenticationService() {
        this.restTemplate = new RestTemplate();
        //this.bioSamplesProperties = bioSamplesProperties;
    }

    public ResponseEntity<SubmissionAccount> getWebinSubmissionAccount(final String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<SubmissionAccount> responseEntity = restTemplate.exchange("https://www.ebi.ac.uk/ena/submit/webin/auth/admin/submission-account/", HttpMethod.GET, entity, SubmissionAccount.class);
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                return responseEntity;
            } else {
                return null;
            }
        } catch (final Exception e) {
            throw new WebinUserLoginUnauthorizedException();
        }
    }

    @ResponseStatus(
            value = HttpStatus.UNAUTHORIZED,
            reason = "Unauthorized WEBIN user")
    private static class WebinUserLoginUnauthorizedException extends RuntimeException {
    }
}
