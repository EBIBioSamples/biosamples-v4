package uk.ac.ebi.biosamples.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Organization implements Comparable<Organization> {

	private String name;
	private String role;
	private String email;
	private String url;
	
	private Organization(String name, String role, String email, String url) {
		this.name = name;
		this.role = role;
		this.email = email;
		this.url = url;
	}

	@JsonProperty("Name") 
	public String getName() {
		return this.name;
	}
	

	@JsonProperty("Role") 
	public String getRole() {
		return this.role;
	}
	

	@JsonProperty("E-mail") 
	public String getEmail() {
		return this.email;
	}
	
	@JsonProperty("URL")
	public String getUrl() {
		return this.url;
	}
	

	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof Organization)) {
            return false;
        }
        Organization other = (Organization) o;
        return Objects.equals(this.name, other.name) 
        		&& Objects.equals(this.role, other.role)
        		&& Objects.equals(this.email, other.email)
        		&& Objects.equals(this.url, other.url);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(name, role, email, url);
    }

	@Override
	public int compareTo(Organization other) {
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

		if (this.role == null && other.role != null) {
			return -1;
		}
		if (this.role != null && other.role == null) {
			return 1;
		}
		if (this.role != null && other.role != null 
				&& !this.role.equals(other.role)) {
			return this.role.compareTo(other.role);
		}

		if (this.email == null && other.email != null) {
			return -1;
		}
		if (this.email != null && other.email == null) {
			return 1;
		}
		if (this.email != null && other.email != null 
				&& !this.email.equals(other.email)) {
			return this.email.compareTo(other.email);
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
	public static Organization build(@JsonProperty("Name") String name, 
			@JsonProperty("Role") String role, 
			@JsonProperty("E-mail") String email, 
			@JsonProperty("URL") String url) {
		return new Organization(name, role,email,url);
	}
}

