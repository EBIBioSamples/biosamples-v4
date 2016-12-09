package uk.ac.ebi.biosamples.models;

import java.util.Objects;

public class Relationship implements Comparable<Relationship>{

	private String type;
	private String target;
	
	public Relationship(){
		
	}

	public String getType() {
		return type;
	}

	public String getTarget() {
		return target;
	}

	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof Relationship)) {
            return false;
        }
        Relationship other = (Relationship) o;
        return Objects.equals(this.type, other.type) 
        		&& Objects.equals(this.target, other.target);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(type, target);
    }
    
	@Override
	public int compareTo(Relationship other) {
		if (other == null) {
			return 1;
		}
		
		if (!this.type.equals(other.type)) {
			return this.type.compareTo(other.type);
		}

		if (!this.target.equals(other.target)) {
			return this.target.compareTo(other.target);
		}
		return 0;
	}
    
    static public Relationship build(String type, String target) {
    	Relationship rel = new Relationship();
    	rel.type = type;
    	rel.target = target;
    	return rel;
    }
}
