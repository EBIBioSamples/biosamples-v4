package uk.ac.ebi.biosamples.mongo.model;

import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.Hashing;

@Document
public class MongoExternalReference implements Comparable<MongoExternalReference> {
	
	private final String url;	
	@Id
	private final String hash;	

	private MongoExternalReference(String url, String hash) {
		this.url = url;
		this.hash = hash;
	}

	public String getUrl() {
		return this.url;
	}
	@JsonIgnore
	public String getHash() {
		return this.hash;
	}
	
	@Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof MongoExternalReference)) {
            return false;
        }
        MongoExternalReference other = (MongoExternalReference) o;
        return Objects.equals(this.url, other.url);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(hash);
    }

	@Override
	public int compareTo(MongoExternalReference other) {
		if (other == null) {
			return 1;
		}
		
		if (!this.url.equals(other.url)) {
			return this.url.compareTo(other.url);
		}
		return 0;
	}	

    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("ExternalReference(");
    	sb.append(this.url);
    	sb.append(")");
    	return sb.toString();
    }


    @JsonCreator
    public static MongoExternalReference build(@JsonProperty("url") String url) {    	
    	UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(url);
    	UriComponents uriComponents = uriComponentsBuilder.build().normalize();

    	url = uriComponents.toUriString();
    	
    	String hash = Hashing.sha256().newHasher()
			.putUnencodedChars(Objects.nonNull(uriComponents.getScheme()) ? uriComponents.getScheme() : "")
			.putUnencodedChars(Objects.nonNull(uriComponents.getSchemeSpecificPart()) ? uriComponents.getSchemeSpecificPart() : "")
			.putUnencodedChars(Objects.nonNull(uriComponents.getUserInfo()) ? uriComponents.getUserInfo() : "")
			.putUnencodedChars(Objects.nonNull(uriComponents.getHost()) ? uriComponents.getHost() : "")
			.putInt(Objects.nonNull(uriComponents.getPort()) ? uriComponents.getPort() : 0)
			.putUnencodedChars(Objects.nonNull(uriComponents.getPath()) ? uriComponents.getPath() : "")
			.putUnencodedChars(Objects.nonNull(uriComponents.getQuery()) ? uriComponents.getQuery() : "")
			.putUnencodedChars(Objects.nonNull(uriComponents.getFragment()) ? uriComponents.getFragment() : "")
			.hash().toString();
    	    	
    	MongoExternalReference externalReference = new MongoExternalReference(url, hash);
		return externalReference;
	}
}
