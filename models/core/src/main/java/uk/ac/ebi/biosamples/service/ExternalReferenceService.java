package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.ExternalReference;

import java.util.Optional;
import java.util.*;

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


    public String getDuoLabel(String duoCode) {
        Hashtable<String, String> my_dict = new Hashtable<String, String>();

        my_dict.put("DUO:0000004", "No restriction");
        my_dict.put("DUO:0000005", "Research & clinical care");
        my_dict.put("DUO:0000006", "Health/(bio)medical/clinical care");
        my_dict.put("DUO:0000007", "Disease-specific & clinical care");
        my_dict.put("DUO:0000011", "Population origins & ancestry");
        my_dict.put("DUO:0000012", "Research-specific");
        my_dict.put("DUO:0000014", "Research use only");
        my_dict.put("DUO:0000015", "No general methods");
        my_dict.put("DUO:0000016", "Genetic studies only");
        my_dict.put("DUO:0000018", "Not-for-profit use only");
        my_dict.put("DUO:0000019", "Publication required");
        my_dict.put("DUO:0000020", "Collaboration required");
        my_dict.put("DUO:0000021", "Ethics approval required");
        my_dict.put("DUO:0000022", "Geographical restriction");
        my_dict.put("DUO:0000024", "Publication moratorium");
        my_dict.put("DUO:0000025", "Time limit on use");
        my_dict.put("DUO:0000026", "User-specific");
        my_dict.put("DUO:0000027", "Project-specific");
        my_dict.put("DUO:0000028", "Institution-specific ");
        my_dict.put("DUO:0000029", "Return to database/resource");

    return my_dict.get(duoCode);
    }
}
