package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = Contact.Builder.class)
public class Contact implements Comparable<Contact> {

	private String firstName;
	private String lastName;
	private String midInitials;
	private String role;
	private String email;
	private String affiliation;
	private String url;
	
	private Contact(String firstName, String lastName, String midInitials,
                    String role, String email, String affiliation, String url) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.midInitials = midInitials;
        this.role = role;
        this.email = email;
        this.affiliation = affiliation;
        this.url = url;
	}

	@JsonProperty("FirstName")
	public String getFirstName() {
		return this.firstName;
	}


    @JsonProperty("LastName")
    public String getLastName() {
        return this.lastName;
    }

    @JsonProperty("MidInitials")
    public String getMidInitials() {
        return this.midInitials;
    }


    @JsonProperty("Name")
    public String getName() { return this.firstName + " " + this.lastName; }

	@JsonProperty("Role")
	public String getRole() {
		return this.role;
	}
		
	@JsonProperty("E-mail")
	public String getEmail() {
		return this.email;
	}

	@JsonProperty("Affiliation")
    public String getAffiliation() {
	    return this.affiliation;
    }

    @JsonProperty("URL")
    public String getUrl() { return this.url; }


	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof Contact)) {
            return false;
        }
        Contact other = (Contact) o;
        return Objects.equals(this.firstName, other.firstName)
        		&& Objects.equals(this.lastName, other.lastName)
                && Objects.equals(this.midInitials, other.midInitials)
                && Objects.equals(this.role, other.role)
        		&& Objects.equals(this.email, other.email)
                && Objects.equals(this.url, other.url)
                && Objects.equals(this.affiliation, other.affiliation);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(firstName, lastName, midInitials, role, email, affiliation, url);
    }

	@Override
	public int compareTo(Contact other) {
		if (other == null) {
			return 1;
		}

		int comparisonResult = nullSafeStringComparison(this.firstName, other.firstName);
		if (comparisonResult != 0) {
		    return comparisonResult;
        }

        comparisonResult = nullSafeStringComparison(this.lastName, other.lastName);
		if (comparisonResult != 0) {
		    return comparisonResult;
        }

        comparisonResult = nullSafeStringComparison(this.midInitials, other.midInitials);
		if (comparisonResult != 0) {
		    return comparisonResult;
        }

        comparisonResult = nullSafeStringComparison(this.role, other.role);
        if (comparisonResult != 0){
		    return comparisonResult;
        }

        comparisonResult = nullSafeStringComparison(this.email, other.email);
        if (comparisonResult != 0) {
            return comparisonResult;
        }

        comparisonResult = nullSafeStringComparison(this.affiliation, other.affiliation);
        return comparisonResult;


	}

	private int nullSafeStringComparison(String first, String other) {
	    if (first == null && other == null) return 0;
	    if (first == null) return -1;
	    if (other == null) return 1;
	    return first.compareTo(other);
    }
		
//	@JsonCreator
//	public static Contact build(@JsonProperty("Name") String name,
//			@JsonProperty("Affiliation") String affiliation,
//			@JsonProperty("URL") String url) {
//		return new Contact(name, affiliation,url);
//	}
    @JsonIgnoreProperties({"Name"})
    public static class Builder {
	    private String firstName;
        private String lastName;
        private String midInitials;
        private String role;
        private String email;
        private String url;
        private String affiliation;

	    @JsonCreator
        public Builder() {}

        @JsonProperty("FirstName")
        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        @JsonProperty("LastName")
        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        @JsonProperty("MidInitials")
        public Builder midInitials(String midInitials) {
            this.midInitials = midInitials;
            return this;
        }

        @JsonProperty("Role")
        public Builder role(String role) {
            this.role = role;
            return this;
        }

        @JsonProperty("E-mail")
        public Builder email(String email) {
            this.email = email;
            return this;
        }

        @JsonProperty("URL")
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        @JsonProperty("Affiliation")
        public Builder affiliation(String affiliation) {
            this.affiliation = affiliation;
            return this;
        }

        public Contact build(){
         return new Contact(firstName, lastName, midInitials, role, email, affiliation, url);
                                                                                                       }
    }
}

