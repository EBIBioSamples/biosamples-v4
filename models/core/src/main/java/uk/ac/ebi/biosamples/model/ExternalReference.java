package uk.ac.ebi.biosamples.model;

import java.nio.charset.Charset;
import java.util.Objects;

import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

public class ExternalReference implements Comparable<ExternalReference> {
	
	private final String url;
	
	private final String urlHash;

	private ExternalReference(String url, String urlHash) {
		this.url = url;
		this.urlHash = urlHash;
	}

	public String getUrl() {
		return this.url;
	}

	@JsonIgnore
	public String getId() {
		return this.urlHash;
	}
	
	@Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ExternalReference)) {
            return false;
        }
        ExternalReference other = (ExternalReference) o;
        return Objects.equals(this.url, other.url);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(urlHash);
    }

	@Override
	public int compareTo(ExternalReference other) {
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
    public static ExternalReference build(@JsonProperty("url") String url) {    	
    	UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(url);
    	UriComponents uriComponents = uriComponentsBuilder.build().normalize();

    	url = uriComponents.toUriString();
    	
    	String urlHash = Hashing.sha256().newHasher()
			.putUnencodedChars(Objects.nonNull(uriComponents.getScheme()) ? uriComponents.getScheme() : "")
			.putUnencodedChars(Objects.nonNull(uriComponents.getSchemeSpecificPart()) ? uriComponents.getSchemeSpecificPart() : "")
			.putUnencodedChars(Objects.nonNull(uriComponents.getUserInfo()) ? uriComponents.getUserInfo() : "")
			.putUnencodedChars(Objects.nonNull(uriComponents.getHost()) ? uriComponents.getHost() : "")
			.putInt(Objects.nonNull(uriComponents.getPort()) ? uriComponents.getPort() : 0)
			.putUnencodedChars(Objects.nonNull(uriComponents.getPath()) ? uriComponents.getPath() : "")
			.putUnencodedChars(Objects.nonNull(uriComponents.getQuery()) ? uriComponents.getQuery() : "")
			.putUnencodedChars(Objects.nonNull(uriComponents.getFragment()) ? uriComponents.getFragment() : "")
			.hash().toString();
    	ExternalReference externalReference = new ExternalReference(url, urlHash);
		return externalReference;
	}
}
