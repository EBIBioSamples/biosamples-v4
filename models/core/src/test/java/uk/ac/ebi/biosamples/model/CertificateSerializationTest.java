package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CertificateSerializationTest {

    private final String exampleJson = "{\"certificates\":[{\"sample\":{\"accession\":\"SAMD00000001\",\"hash\":\"92B20C97021A0E9B232F8549FE1B5496\"},\"checklist\":{\"name\":\"ncbi\",\"version\":\"0.0.1\",\"file\":\"schemas/ncbi-candidate-schema.json\"},\"curations\":[]},{\"sample\":{\"accession\":\"SAMD00000001\",\"hash\":\"92B20C97021A0E9B232F8549FE1B5496\"},\"checklist\":{\"name\":\"ncbi\",\"version\":\"0.0.1\",\"file\":\"schemas/ncbi-candidate-schema.json\"},\"curations\":[{\"characteristic\":\"INSDC status\",\"before\":\"live\",\"after\":\"public\",\"applied\":true}]}]}";

    @Test
    public void test_serialize_certificate() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        CertificationResponse certificationResponse = objectMapper.readValue(exampleJson, CertificationResponse.class);
        assertEquals(2, certificationResponse.getCertificates().size());
        for (Certificate certificate : certificationResponse.getCertificates()) {
            assertNotNull(certificate);
            Certificate.CertificateSample sample = certificate.getCertificateSample();
            assertNotNull(sample);
            Certificate.CertificateChecklist checklist = certificate.getCertificateChecklist();
            assertNotNull(checklist);
            List<Certificate.CertificateCuration> curations = certificate.getCertificateCurations();
            assertNotNull(curations);
        }
        System.out.println(certificationResponse.toString());
    }

}
