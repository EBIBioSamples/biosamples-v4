package uk.ac.ebi.biosamples.mongo.model;

import java.nio.charset.Charset;
import java.util.*;

import com.google.common.hash.Hasher;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.Hashing;

public class MongoExternalReference implements Comparable<MongoExternalReference> {
	
	private final String url;	
	private final String hash;
	private final SortedSet<String> duo;

	private MongoExternalReference(String url, String hash, SortedSet<String> duo) {
		this.url = url;
		this.hash = hash;
		this.duo = duo;
	}

	public String getUrl() {
		return this.url;
	}
	@JsonIgnore
	public String getHash() {
		return this.hash;
	}

	public SortedSet<String> getDuo() {
		return duo;
	}
	
	@Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof MongoExternalReference)) {
            return false;
        }
        MongoExternalReference other = (MongoExternalReference) o;
        return Objects.equals(this.url, other.url) && Objects.equals(this.duo, other.duo);
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
		if (!this.duo.equals(other.duo)) {
			if (this.duo.size() < other.duo.size()) {
				return -1;
			} else if (this.duo.size() > other.duo.size()) {
				return 1;
			} else {
				Iterator<String> thisIt = this.duo.iterator();
				Iterator<String> otherIt = other.duo.iterator();
				while (thisIt.hasNext() && otherIt.hasNext()) {
					int val = thisIt.next().compareTo(otherIt.next());
					if (val != 0) return val;
				}
			}
		}
		return 0;
	}	

    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("ExternalReference(");
    	sb.append(this.url);
		sb.append(",");
		sb.append(duo);
    	sb.append(")");
    	return sb.toString();
    }

    public static MongoExternalReference build(String url, SortedSet<String> duo) {
		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(url);
		UriComponents uriComponents = uriComponentsBuilder.build().normalize();

		url = uriComponents.toUriString();

		Hasher hasher = Hashing.sha256().newHasher()
				.putUnencodedChars(Objects.nonNull(uriComponents.getScheme()) ? uriComponents.getScheme() : "")
				.putUnencodedChars(Objects.nonNull(uriComponents.getSchemeSpecificPart()) ? uriComponents.getSchemeSpecificPart() : "")
				.putUnencodedChars(Objects.nonNull(uriComponents.getUserInfo()) ? uriComponents.getUserInfo() : "")
				.putUnencodedChars(Objects.nonNull(uriComponents.getHost()) ? uriComponents.getHost() : "")
				.putInt(Objects.nonNull(uriComponents.getPort()) ? uriComponents.getPort() : 0)
				.putUnencodedChars(Objects.nonNull(uriComponents.getPath()) ? uriComponents.getPath() : "")
				.putUnencodedChars(Objects.nonNull(uriComponents.getQuery()) ? uriComponents.getQuery() : "")
				.putUnencodedChars(Objects.nonNull(uriComponents.getFragment()) ? uriComponents.getFragment() : "");

		for (String s : duo) {
			hasher.putUnencodedChars(s);
		}

		return new MongoExternalReference(url, hasher.hash().toString(), duo);
	}


    @JsonCreator
    public static MongoExternalReference build(@JsonProperty("url") String url) {    	
    	return build(url, new TreeSet<>());
	}
}
