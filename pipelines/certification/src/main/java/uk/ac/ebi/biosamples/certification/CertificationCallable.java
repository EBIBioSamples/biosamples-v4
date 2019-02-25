package uk.ac.ebi.biosamples.certification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.model.CertificationResponse;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.concurrent.Callable;

public class CertificationCallable implements Callable<Void> {

    private Logger log = LoggerFactory.getLogger(getClass());

    private final RestTemplate restTemplate;

    private final Sample sample;

    private final PipelinesProperties piplinesProperties;

    public CertificationCallable(RestTemplate restTemplate, Sample sample, PipelinesProperties piplinesProperties) {
        this.restTemplate = restTemplate;
        this.sample = sample;
        this.piplinesProperties = piplinesProperties;
    }

    @Override
    public Void call() throws Exception {
        try {
            CertificationResponse certificationResponse = restTemplate.postForObject(piplinesProperties.getCertificationUri(), sample, CertificationResponse.class);
            log.info(certificationResponse.toString());
        } catch (RestClientException e) {
            e.printStackTrace();
        }
        return null;
    }
}
