package uk.ac.ebi.biosamples.certification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;

import java.util.concurrent.Callable;

public class CertificationCallable implements Callable<Void> {

    private Logger log = LoggerFactory.getLogger(getClass());

    private final RestTemplate restTemplate;

    private final Sample sample;

    private final PipelinesProperties piplinesProperties;

    private final String domain;

    private final BioSamplesClient bioSamplesClient;

    public CertificationCallable(BioSamplesClient bioSamplesClient, RestTemplate restTemplate, Sample sample, PipelinesProperties piplinesProperties) {
        this.bioSamplesClient = bioSamplesClient;
        this.restTemplate = restTemplate;
        this.sample = sample;
        this.piplinesProperties = piplinesProperties;
        this.domain = piplinesProperties.getCertificationDomain();
    }

    @Override
    public Void call() throws Exception {
        try {
            CertificationResponse certificationResponse = restTemplate.postForObject(piplinesProperties.getCertificationUri(), sample, CertificationResponse.class);
            submitCurations(certificationResponse);
        } catch (RestClientException e) {
            log.error(String.format("certification of %s by %s failed with %s", sample.getAccession(), piplinesProperties.getCertificationUri(), e.getMessage()));
        }
        return null;
    }

    private void submitCurations(CertificationResponse certificationResponse) {
        for (Certificate certificate : certificationResponse.getCertificates()) {
            for (Certificate.CertificateCuration certificationCuration : certificate.getCertificateCurations()) {
                Attribute attributePre = Attribute.build(certificationCuration.getCharacteristic(), certificationCuration.getBefore());
                Attribute attributePost = Attribute.build(certificationCuration.getCharacteristic(), certificationCuration.getAfter());
                Curation curation = Curation.build(attributePre, attributePost);
                Resource<CurationLink> response = bioSamplesClient.persistCuration(sample.getAccession(), curation, domain);
                log.info(response.toString());
            }
            //TODO:record certitifcate
        }
    }
}
