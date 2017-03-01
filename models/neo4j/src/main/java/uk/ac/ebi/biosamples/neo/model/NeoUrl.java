package uk.ac.ebi.biosamples.neo.model;

import java.net.URI;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Index;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import uk.ac.ebi.biosamples.neo.service.URIConverter;

@NodeEntity(label = "Url")
public class NeoUrl {

	@GraphId
	private Long id;

	@Property
	@Index(unique=true, primary=true)
	@Convert(URIConverter.class)
	private URI url;


	private NeoUrl( URI url) {
		this.url = url;
	}

	public Long getId() {
		return id;
	}	
	
	public URI getUrl(){
		return url;
	}

    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("NeoUrl(");
    	sb.append(url);
    	sb.append(")");
    	return sb.toString();
    }
    
	public static NeoUrl create(URI url) {
		NeoUrl neoUrl = new NeoUrl(url);
		return neoUrl;
	}
}
