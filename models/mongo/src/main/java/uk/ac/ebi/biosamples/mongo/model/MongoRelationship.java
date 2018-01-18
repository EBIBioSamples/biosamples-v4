package uk.ac.ebi.biosamples.mongo.model;

import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

@Document
public class MongoRelationship implements Comparable<MongoRelationship> {

	@Id
	private final String hash;
	
	private final String type;
	@Indexed(background=true)
	private final String target;
	@Indexed(background=true)
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
		
		if (!Objects.equals(this.type, other.type)) {
			return this.type.compareTo(other.type);
		}

		if (!Objects.equals(this.target, other.target)) {
			return this.target.compareTo(other.target);
		}

		if (!Objects.equals(this.source, other.source)) {
			if (this.source == null && other.source != null) {
				return 1;
			} else if (this.source != null && other.source == null) {
				return -1;
			} else {
				return this.source.compareTo(other.source);
			}
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

    	Hasher hasher = Hashing.sha256().newHasher();
    	hasher.putUnencodedChars(type);
    	hasher.putUnencodedChars(target);
    	if (source != null) {
    		hasher.putUnencodedChars(source);
    	}
    	
    	String hash = hasher.hash().toString();     	
    	
    	MongoRelationship rel = new MongoRelationship(type, target, source, hash);
    	return rel;
    }
}
