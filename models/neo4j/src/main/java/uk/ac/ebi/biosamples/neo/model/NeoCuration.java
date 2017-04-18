package uk.ac.ebi.biosamples.neo.model;

import java.util.Set;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity(label = "Curation")
public class NeoCuration {

	@GraphId
	private Long id;

    @Relationship(type = "HAS_PRE_ATTRIBUTE")
	private Set<NeoAttribute> attributesPre;
    
    @Relationship(type = "HAS_POST_ATTRIBUTE")    
	private Set<NeoAttribute> attributesPost;

	@SuppressWarnings("unused")
	private NeoCuration() {
	}
	
	public Long getId() {
		return id;
	}

	public Set<NeoAttribute> getAttributesPre() {
		return attributesPre;
	}

	public Set<NeoAttribute> getAttributesPost() {
		return attributesPost;
	}
}
