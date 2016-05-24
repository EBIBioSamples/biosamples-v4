package uk.ac.ebi.biosamples.models;

import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = SampleSerializer.class)
@JsonDeserialize(using = SampleDeserializer.class)
public interface Sample {

	public String getAccession();

	public String getName();

	public Set<String> getAttributeTypes();

	public Set<String> getAttributeValues(String type);

	public String getAttributeUnit(String type, String value);

	public String getAttributeOntologyTerm(String type, String value);

}
