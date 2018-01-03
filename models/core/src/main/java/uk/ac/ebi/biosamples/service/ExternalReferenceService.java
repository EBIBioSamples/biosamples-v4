package uk.ac.ebi.biosamples.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.model.ExternalReference;

@Service
public class ExternalReferenceService {
	
	public String getNickname(ExternalReference externalReference) {
		//TODO make this more configurable
		if (externalReference.getUrl().contains("www.ebi.ac.uk/ena")) {
			return "ENA";
		} else if (externalReference.getUrl().contains("www.ebi.ac.uk/arrayexpress")) {
			return "ArrayExpress";
		} else if (externalReference.getUrl().contains("hpscreg.eu/")) {
			return "hPSCreg";
		} else {
			return "other";
		}
	}

	public Optional<String> getDataId(ExternalReference externalReference) {
		
		String nickname = getNickname(externalReference);		
		if ("ENA".equals(nickname)) {
			UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(externalReference.getUrl()).build();
			String lastPathSegment = uriComponents.getPathSegments().get(uriComponents.getPathSegments().size()-1);
			return Optional.of(lastPathSegment);
		}
		if ("ArrayExpress".equals(nickname)) {
			UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(externalReference.getUrl()).build();
			String lastPathSegment = uriComponents.getPathSegments().get(uriComponents.getPathSegments().size()-1);
			return Optional.of(lastPathSegment);	
		}
		if ("hPSCreg".equals(nickname)) {
			UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(externalReference.getUrl()).build();
			String lastPathSegment = uriComponents.getPathSegments().get(uriComponents.getPathSegments().size()-1);
			return Optional.of(lastPathSegment);
		}
        return Optional.empty();
	}
}
