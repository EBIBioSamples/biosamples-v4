package uk.ac.ebi.biosamples.mongo.service;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Certificate;
import uk.ac.ebi.biosamples.mongo.model.MongoCertificate;

@Service
@ConfigurationPropertiesBinding
public class MongoCertificateToCertificateConverter implements Converter<MongoCertificate, Certificate> {
    @Override
    public Certificate convert(MongoCertificate mongoCertificate) {
        return Certificate.build(mongoCertificate.getName(), mongoCertificate.getVersion(), mongoCertificate.getFileName());
    }
}
