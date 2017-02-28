package uk.ac.ebi.biosamples.neo.model;

import java.util.Objects;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

@RelationshipEntity(type = "RELATED_TO")
public class NeoRelationship  {

	@GraphId
	private Long id;
	
	@StartNode
	private NeoSample owner;
	@EndNode
	private NeoSample target;

	@Property
	private String specificType;

	private NeoRelationship() {
	};

	public Long getId() {
		return id;
	};

	public String getSpecificType() {
		return specificType;
	}

	public NeoSample getOwner() {
		return owner;
	}

	public NeoSample getTarget() {
		return target;
	}

	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof NeoRelationship)) {
            return false;
        }
        NeoRelationship other = (NeoRelationship) o;
        return Objects.equals(this.specificType, other.specificType) 
        		&& Objects.equals(this.target, other.target)
        		&& Objects.equals(this.owner, other.owner);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(specificType, target, owner);
    }
	

	public static NeoRelationship create(NeoSample owner, NeoSample target, String specificType) {
		NeoRelationship newRelationship = new NeoRelationship();
		newRelationship.owner = owner;
		newRelationship.target = target;
		newRelationship.specificType = specificType;
		return newRelationship;
	}
}
