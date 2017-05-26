package uk.ac.ebi.biosamples.neo.model;


import java.util.Objects;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Index;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.hash.Hashing;

@NodeEntity(label = "ExternalReference")
public class NeoExternalReference implements Comparable<NeoExternalReference>{

	@GraphId
	private Long id;

	@Property
	@Index(unique=true, primary=true)
	private String url;

	@Property
	@Index(unique=true, primary=false)	
	private String urlHash;


	private NeoExternalReference() {
	}
	
	private NeoExternalReference(String url, String urlHash) {
		this.url = url;
		this.urlHash = urlHash;
	}
	
	public Long getId() {
		return id;
	}	
	
	public String getUrl() {
		return url;
	}
	
	public String getUrlHash() {
		return urlHash;
	}
	
	@Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof NeoExternalReference)) {
            return false;
        }
        NeoExternalReference other = (NeoExternalReference) o;
        return Objects.equals(this.url, other.url);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(url);
    }

    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("NeoExternalReference(");
    	sb.append(url);
    	sb.append(")");
    	return sb.toString();
    }

	@Override
	public int compareTo(NeoExternalReference other) {
		if (other == null) {
			return 1;
		}
		if (!this.url.equals(other.url)) {
			return this.url.compareTo(other.url);
		}
		return 0;
	}
    
	public static NeoExternalReference build(String url) {	
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
    	
		NeoExternalReference neoUrl = new NeoExternalReference(url, urlHash);
		return neoUrl;
	}
}
