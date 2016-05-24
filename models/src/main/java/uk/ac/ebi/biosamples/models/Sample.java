package uk.ac.ebi.biosamples.models;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

public interface Sample {

	public String getAccession();
	public String getName();
	public Set<String> getAttributeTypes();
	public Set<String> getAttributeValues(String type);
	public Optional<String> getAttributeUnit(String type, String value);
	public Optional<URI> getAttributeOntologyTerm(String type, String value);
	
}
