package uk.ac.ebi.biosamples.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Relationship implements Comparable<Relationship> {

	private final String type;
	private final String target;
	private final String source;
	
	private Relationship(String type, String target, String source){
		this.type = type;
		this.target = target;
		this.source = source;
		
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

    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("Relationships(");
    	sb.append(source);
    	sb.append(",");
    	sb.append(type);
    	sb.append(",");
    	sb.append(target);
    	sb.append(")");
    	return sb.toString();
    }
    
    @JsonCreator
    public static Relationship build(@JsonProperty("source") String source, 
    		@JsonProperty("type") String type,
    		@JsonProperty("target") String target) {
    	if (type == null || type.trim().length() == 0) throw new IllegalArgumentException("type cannot be empty");
    	if (target == null || target.trim().length() == 0) throw new IllegalArgumentException("target cannot be empty");
    	if (source == null || source.trim().length() == 0) throw new IllegalArgumentException("source cannot be empty");
    	Relationship rel = new Relationship(type, target, source);
    	return rel;
    }

    public static class Builder {
		private String source;
		private String target;
		private String type;

		public Builder() {}

		public Builder withSource(String source) {
			this.source = source;
			return this;
		}

		public Builder withTarget(String target) {
			this.target = target;
			return this;
		}

		public Builder withType(String type) {
			this.type = type;
			return this;
		}

		public Relationship build() {
			if (type == null || type.trim().length() == 0) throw new IllegalArgumentException("type cannot be empty");
			if (target == null || target.trim().length() == 0) throw new IllegalArgumentException("target cannot be empty");
			if (source == null || source.trim().length() == 0) throw new IllegalArgumentException("source cannot be empty");
			return Relationship.build(source, type, target);
		}

	}
}
