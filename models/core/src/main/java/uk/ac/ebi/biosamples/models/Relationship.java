package uk.ac.ebi.biosamples.models;

import java.util.Objects;

public class Relationship implements Comparable<Relationship>{

	private String type;
	private String target;
	private String source;
	
	public Relationship(){
		
	}

	public String getType() {
		return type;
	}

	public String getTarget() {
		return target;
	}

	public String getSource() {
		return source;
	}

	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof Relationship)) {
            return false;
        }
        Relationship other = (Relationship) o;
        return Objects.equals(this.type, other.type) 
        		&& Objects.equals(this.target, other.target)
        		&& Objects.equals(this.source, other.source);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(type, target, source);
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

		if (!this.source.equals(other.source)) {
			return this.source.compareTo(other.source);
		}
		return 0;
	}
    
    static public Relationship build(String type, String target, String source) {
    	Relationship rel = new Relationship();
    	rel.type = type;
    	rel.target = target;
    	rel.source = source;
    	return rel;
    }
}
