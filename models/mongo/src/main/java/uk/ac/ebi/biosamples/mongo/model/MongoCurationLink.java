package uk.ac.ebi.biosamples.mongo.model;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.hash.Hashing;

import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.service.CustomLocalDateTimeDeserializer;
import uk.ac.ebi.biosamples.service.CustomLocalDateTimeSerializer;

@Document
public class MongoCurationLink implements Comparable<MongoCurationLink>{

	@Id
	private final String hash;
	
	@Indexed
	private final String sample;
	
	@Indexed
	protected final LocalDateTime created;

	private final Curation curation;

	private MongoCurationLink(String sample, Curation curation, String hash, LocalDateTime created) {
		this.sample = sample;
		this.curation = curation;
		this.hash = hash;
		this.created = created;
	}

	public String getSample() {
		return sample;
	}
	
	public Curation getCuration() {
		return curation;
	}
	
	public String getHash() {
		return hash;
	}

	@JsonSerialize(using = CustomLocalDateTimeSerializer.class)
	public LocalDateTime getCreated() {
		return created;
	}

	@Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof CurationLink)) {
            return false;
        }
        MongoCurationLink other = (MongoCurationLink) o;
        return Objects.equals(this.curation, other.curation)
        		&& Objects.equals(this.sample, other.sample);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(sample, curation);
    }

	@Override
	public int compareTo(MongoCurationLink other) {
		if (other == null) {
			return 1;
		}
		
		if (!this.sample.equals(other.sample)) {
			return this.sample.compareTo(other.sample);
		}
		if (!this.curation.equals(other.curation)) {
			return this.curation.compareTo(other.curation);
		}
		return 0;
	}	

    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("MongoCurationLink(");
    	sb.append(this.sample);
    	sb.append(",");
    	sb.append(this.curation);
    	sb.append(")");
    	return sb.toString();
    }

    //Used for deserializtion (JSON -> Java)
    @JsonCreator
	public static MongoCurationLink build(@JsonProperty("sample") String sample, @JsonProperty("curation") Curation curation,
			@JsonProperty("created") @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class) LocalDateTime created) {

    	String hash = Hashing.sha256().newHasher()
			.putUnencodedChars(curation.getHash())
			.putUnencodedChars(sample)
			.hash().toString();
    	
		return new MongoCurationLink(sample, curation, hash, created);
	}

}
