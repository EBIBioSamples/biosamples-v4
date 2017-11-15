package uk.ac.ebi.biosamples.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Contact implements Comparable<Contact> {

	private String name;
	private String affiliation;
	private String url;
	
	private Contact(String name, String affiliation, String url) {
		this.name = name;
		this.affiliation = affiliation;
		this.url = url;
	}

	@JsonProperty("Name") 
	public String getName() {
		return this.name;
	}
	

	@JsonProperty("Affiliation") 
	public String getAffiliation() {
		return this.affiliation;
	}
		
	@JsonProperty("URL")
	public String getUrl() {
		return this.url;
	}

	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof Contact)) {
            return false;
        }
        Contact other = (Contact) o;
        return Objects.equals(this.name, other.name) 
        		&& Objects.equals(this.affiliation, other.affiliation)
        		&& Objects.equals(this.url, other.url);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(name, affiliation, url);
    }

	@Override
	public int compareTo(Contact other) {
		if (other == null) {
			return 1;
		}

		if (this.name == null && other.name != null) {
			return -1;
		}
		if (this.name != null && other.name == null) {
			return 1;
		}
		if (this.name != null && other.name != null 
				&& !this.name.equals(other.name)) {
			return this.name.compareTo(other.name);
		}

		if (this.affiliation == null && other.affiliation != null) {
			return -1;
		}
		if (this.affiliation != null && other.affiliation == null) {
			return 1;
		}
		if (this.affiliation != null && other.affiliation != null 
				&& !this.affiliation.equals(other.affiliation)) {
			return this.affiliation.compareTo(other.affiliation);
		}

		if (this.url == null && other.url != null) {
			return -1;
		}
		if (this.url != null && other.url == null) {
			return 1;
		}
		if (this.url != null && other.url != null 
				&& !this.url.equals(other.url)) {
			return this.url.compareTo(other.url);
		}
		//no differences, must be the same
		return 0;
	}
		
		
	@JsonCreator
	public static Contact build(@JsonProperty("Name") String name, 
			@JsonProperty("Affiliation") String affiliation, 
			@JsonProperty("URL") String url) {
		return new Contact(name, affiliation,url);
	}
}

