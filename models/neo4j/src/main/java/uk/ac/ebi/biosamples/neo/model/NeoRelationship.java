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
	private String type;

	private NeoRelationship() {
	};

	public Long getId() {
		return id;
	};

	public String getType() {
		return type;
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
        return Objects.equals(this.type, other.type) 
        		&& Objects.equals(this.target.getAccession(), other.target.getAccession())
        		&& Objects.equals(this.owner.getAccession(), other.owner.getAccession());
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(type, target, owner);
    }	

	public static NeoRelationship build(NeoSample owner, String specificType, NeoSample target) {
		NeoRelationship newRelationship = new NeoRelationship();
		newRelationship.owner = owner;
		newRelationship.target = target;
		newRelationship.type = specificType;
		return newRelationship;
	}
}
