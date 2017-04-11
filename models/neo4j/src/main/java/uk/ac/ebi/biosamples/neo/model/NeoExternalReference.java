package uk.ac.ebi.biosamples.neo.model;


import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Index;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;

@NodeEntity(label = "ExternalReference")
public class NeoExternalReference {

	@GraphId
	private Long id;

	@Property
	@Index(unique=true, primary=true)
	private String url;

	private NeoExternalReference() {}
	
	public Long getId() {
		return id;
	}	
	
	public String getUrl(){
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
    
	public static NeoExternalReference build(String url) {
		NeoExternalReference neoUrl = new NeoExternalReference();
		neoUrl.url = url;
		return neoUrl;
	}
}
