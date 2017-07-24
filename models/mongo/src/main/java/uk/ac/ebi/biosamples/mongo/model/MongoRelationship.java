package uk.ac.ebi.biosamples.mongo.model;

import java.util.Objects;

import org.springframework.data.annotation.Id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.Hashing;

public class MongoRelationship implements Comparable<MongoRelationship> {

	@Id
	private final String hash;
	
	private final String type;
	private final String target;
	private final String source;
	
	private MongoRelationship(String type, String target, String source, String hash){
		this.type = type;
		this.target = target;
		this.source = source;
		this.hash = hash;
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

	public String getHash() {
		return hash;
	}

	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof MongoRelationship)) {
            return false;
        }
        MongoRelationship other = (MongoRelationship) o;
        return Objects.equals(this.type, other.type) 
        		&& Objects.equals(this.target, other.target)
        		&& Objects.equals(this.source, other.source);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(type, target, source);
    }
    
	@Override
	public int compareTo(MongoRelationship other) {
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
    public static MongoRelationship build(@JsonProperty("source") String source, 
    		@JsonProperty("type") String type,
    		@JsonProperty("target") String target) {
    	if (type == null || type.trim().length() == 0) throw new IllegalArgumentException("type cannot be empty");
    	if (target == null || target.trim().length() == 0) throw new IllegalArgumentException("target cannot be empty");
    	if (source == null || source.trim().length() == 0) throw new IllegalArgumentException("source cannot be empty");

    	String hash = Hashing.sha256().newHasher()
			.putUnencodedChars(type)
			.putUnencodedChars(target)
			.putUnencodedChars(source)
			.hash().toString();
    	
    	
    	MongoRelationship rel = new MongoRelationship(type, target, source, hash);
    	return rel;
    }
}
