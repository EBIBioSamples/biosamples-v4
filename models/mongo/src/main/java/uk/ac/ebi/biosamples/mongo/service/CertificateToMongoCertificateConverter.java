package uk.ac.ebi.biosamples.mongo.service;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Certificate;
import uk.ac.ebi.biosamples.mongo.model.MongoCertificate;

@Service
public class CertificateToMongoCertificateConverter implements Converter<Certificate, MongoCertificate> {
    @Override
    public MongoCertificate convert(Certificate cert) {
        return MongoCertificate.build(cert.getName(), cert.getVersion(), cert.getFileName());
    }
}
