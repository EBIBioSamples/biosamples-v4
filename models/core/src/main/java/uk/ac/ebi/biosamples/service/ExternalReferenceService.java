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
		} else if (externalReference.getUrl().contains("ncbi.nlm.nih.gov/projects/gap")) {
			return "dbGaP";
		} else if (externalReference.getUrl().contains("ega-archive.org/datasets")) {
			return "EGA Dataset";
		} else if (externalReference.getUrl().contains("ega-archive.org/metadata")) {
			return "EGA Sample";
		} else if (externalReference.getUrl().contains("ebi.ac.uk/biostudies")) {
			return "BioStudies";
		} else {
			return "other";
		}
	}

	public Optional<String> getDataId(ExternalReference externalReference) {
		
		String nickname = getNickname(externalReference);		
		if ("ENA".equals(nickname) || "ArrayExpress".equals(nickname) || "hPSCreg".equals(nickname)
				|| "EGA Dataset".equals(nickname) || "EGA Sample".equals(nickname) || "BioStudies".equals(nickname)) {
			UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(externalReference.getUrl()).build();
			String lastPathSegment = uriComponents.getPathSegments().get(uriComponents.getPathSegments().size()-1);
			return Optional.of(lastPathSegment);
		} else if ("dbGaP".equals(nickname)) {
			UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(externalReference.getUrl()).build();
			String studyId = uriComponents.getQueryParams().getFirst("study_id");
			return Optional.of(studyId);
		}
		return Optional.empty();
	}
}
