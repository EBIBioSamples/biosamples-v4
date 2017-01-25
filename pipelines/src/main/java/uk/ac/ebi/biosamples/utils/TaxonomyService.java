package uk.ac.ebi.biosamples.utils;

import java.net.URI;

import org.springframework.stereotype.Service;

@Service
public class TaxonomyService {

	public URI getUriForTaxonId(int taxonId) {
		return URI.create("http://purl.obolibrary.org/obo/NCBITaxon_"+taxonId);
	}
}
