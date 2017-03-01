package uk.ac.ebi.biosamples.model;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

public class ExternalReference implements Comparable<ExternalReference> {

	protected URL url;	
	
	public URL getUrl() {
		return url;
	}
	
	@Override
	public int compareTo(ExternalReference other) {
		if (other == null) {
			return 1;
		}
		
		if (!this.url.equals(other.url)) {
			return this.url.toExternalForm().compareTo(other.url.toExternalForm());
		}
		
		return 0;		
	}

	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof ExternalReference)) {
            return false;
        }
        ExternalReference other = (ExternalReference) o;
        return Objects.equals(this.url, other.url) ;
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(url);
    }
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("ExternalReference(");
    	sb.append(url);
    	sb.append(")");
    	return sb.toString();
    }
    
    static public ExternalReference build(String string) {
    	try {
			return ExternalReference.build(URI.create(string).toURL());
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
    }
    
    static public ExternalReference build(URL url) {
    	ExternalReference externalReference = new ExternalReference();
    	externalReference.url = url;
    	return externalReference;
    }
}
