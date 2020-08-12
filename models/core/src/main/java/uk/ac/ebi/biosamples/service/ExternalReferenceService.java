package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.ExternalReference;

import java.util.Optional;
import java.util.*;

@Service
public class ExternalReferenceService {
    private static final Map<String, String> duoMap = Map.ofEntries(Map.entry("DUO:0000004", "No restriction"), Map.entry("DUO:0000005", "Research & clinical care"), Map.entry("DUO:0000006", "Health/(bio)medical/clinical care"), Map.entry("DUO:0000007", "Disease-specific & clinical care"), Map.entry("DUO:0000011", "Population origins & ancestry"), Map.entry("DUO:0000012", "Research-specific"), Map.entry("DUO:0000014", "Research use only"), Map.entry("DUO:0000015", "No general methods"), Map.entry("DUO:0000016", "Genetic studies only"), Map.entry("DUO:0000018", "Not-for-profit use only"), Map.entry("DUO:0000019", "Publication required"), Map.entry("DUO:0000020", "Collaboration required"), Map.entry("DUO:0000021", "Ethics approval required"), Map.entry("DUO:0000022", "Geographical restriction"), Map.entry("DUO:0000024", "Publication moratorium"), Map.entry("DUO:0000025", "Time limit on use"), Map.entry("DUO:0000026", "User-specific"), Map.entry("DUO:0000027", "Project-specific"), Map.entry("DUO:0000028", "Institution-specific "), Map.entry("DUO:0000029", "Return to database/resource"));

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
