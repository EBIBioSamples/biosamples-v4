package uk.ac.ebi.biosamples.models;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

@RelationshipEntity(type = "RELATED_TO")
public class NeoRelationship {

	@GraphId
	private Long id;

	@Property
	private String specificType;
	@StartNode
	private NeoSample owner;
	@EndNode
	private NeoSample target;

	private NeoRelationship() {
	};

	public Long getId() {
		return id;
	};

	public String getSpecificType() {
		return specificType;
	}

	public void setSpecificType(String specificType) {
		this.specificType = specificType;
	}

	public static NeoRelationship create(NeoSample owner, NeoSample target, String specificType) {
		NeoRelationship newRelationship = new NeoRelationship();
		newRelationship.owner = owner;
		newRelationship.target = target;
		newRelationship.specificType = specificType;
		return newRelationship;
	}
}
