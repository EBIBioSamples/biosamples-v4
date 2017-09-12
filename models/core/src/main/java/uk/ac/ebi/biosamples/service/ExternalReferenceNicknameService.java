package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.ExternalReference;

@Service
public class ExternalReferenceNicknameService {

	
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
}
