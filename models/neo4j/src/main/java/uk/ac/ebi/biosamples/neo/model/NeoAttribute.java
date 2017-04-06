package uk.ac.ebi.biosamples.neo.model;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Index;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;

@NodeEntity(label = "Attribute")
public class NeoAttribute {

	@GraphId
	private Long id;

	@Property
	@Index
	private String type;

	@Property
	@Index
	private String value;

	@Property
	@Index
	private String iri;

	@Property
	@Index
	private String unit;
	
	@Property
	@Index(unique=true, primary=true)
	private String compositeIdentifier;
	
	
	private NeoAttribute() {
		
	}

	public String getType() {
		return type;
	}

	public String getValue() {
		return value;
	}

	public String getIri() {
		return iri;
	}

	public String getUnit() {
		return unit;
	}
	
	public static NeoAttribute build(String type, String value, String iri, String unit) {
		NeoAttribute neoAttribute = new NeoAttribute();
		neoAttribute.type = type;
		neoAttribute.value = value;
		neoAttribute.iri = iri;
		neoAttribute.unit = unit;
		
		//build the composite identifier here so that neo4j can use it as a primary index
		//to unique and merge nodes
		String compositeIdentifier = type+"_"+value;
		if (iri != null)  {
			compositeIdentifier = compositeIdentifier+"_"+iri;
		}
		if (unit != null)  {
			compositeIdentifier = compositeIdentifier+"_"+unit;
		}
		neoAttribute.compositeIdentifier = compositeIdentifier;
		return neoAttribute;
	}
}
