package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.ExternalReference;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

@Service
public class ExternalReferenceService {

	
	public String getNickname(ExternalReference externalReference) {
		//TODO make this more configurable
		if (externalReference.getUrl().contains("www.ebi.ac.uk/ena")) {
			return "ENA";
		} else if (externalReference.getUrl().contains("www.ebi.ac.uk/arrayexpress")) {
			return "ArrayExpress";
		} else {
			return "other";
		}
	}

	public Optional<String> getDataId(ExternalReference externalReference) {
        try {
            URL erUrl = new URL(externalReference.getUrl());
            if (isEbiUrl(erUrl)) {
                return Optional.of(getEbiExternalReferenceDataId(erUrl));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
	}

	private boolean isEbiUrl(URL url) {
        return url.getHost().contains("ebi.ac.uk");
    }

    private String getEbiServiceName(URL ebiUrl) {
	    String[] subPaths = ebiUrl.getPath().substring(1).split("/");
	    return subPaths[0];
    }

    private String getEbiExternalReferenceDataId(URL externalReferenceDataUrl) {
        String[] subPaths = externalReferenceDataUrl.getPath().substring(1).split("/");
        return subPaths[subPaths.length - 1];

    }
}
