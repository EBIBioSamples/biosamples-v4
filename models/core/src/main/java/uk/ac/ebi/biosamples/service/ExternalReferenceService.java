package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.ExternalReference;

import java.util.Optional;
import java.util.*;

@Service
public class ExternalReferenceService {
    private static final Map<String, String> duoMap = new HashMap<>() {{
        put("DUO:0000004", "No restriction");
        put("DUO:0000005", "Research & clinical care");
        put("DUO:0000006", "Health/(bio)medical/clinical care");
        put("DUO:0000007", "Disease-specific & clinical care");
        put("DUO:0000011", "Population origins & ancestry");
        put("DUO:0000012", "Research-specific");
        put("DUO:0000014", "Research use only");
        put("DUO:0000015", "No general methods");
        put("DUO:0000016", "Genetic studies only");
        put("DUO:0000018", "Not-for-profit use only");
        put("DUO:0000019", "Publication required");
        put("DUO:0000020", "Collaboration required");
        put("DUO:0000021", "Ethics approval required");
        put("DUO:0000022", "Geographical restriction");
        put("DUO:0000024", "Publication moratorium");
        put("DUO:0000025", "Time limit on use");
        put("DUO:0000026", "User-specific");
        put("DUO:0000027", "Project-specific");
        put("DUO:0000028", "Institution-specific ");
        put("DUO:0000029", "Return to database/resource");
    }};

    public String getNickname(ExternalReference externalReference) {
        return ExternalReferenceUtils.getNickname(externalReference);
    }

    public Optional<String> getDataId(ExternalReference externalReference) {
        return ExternalReferenceUtils.getDataId(externalReference);
    }

    public String getDuoUrl(String duoCode) {
        return ExternalReferenceUtils.getDuoUrl(duoCode);
    }


    public String getDuoLabel(String duoCode) {
        return duoMap.get(duoCode);
    }
}
