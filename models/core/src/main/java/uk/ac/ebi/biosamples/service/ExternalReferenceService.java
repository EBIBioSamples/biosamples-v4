package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.ExternalReference;

import java.util.Optional;

@Service
public class ExternalReferenceService {
    public String getNickname(ExternalReference externalReference) {
        return ExternalReferenceUtils.getNickname(externalReference);
    }

    public Optional<String> getDataId(ExternalReference externalReference) {
        return ExternalReferenceUtils.getDataId(externalReference);
    }

    public String getDuoUrl(String duoCode) {
        return ExternalReferenceUtils.getDuoUrl(duoCode);
    }
}
