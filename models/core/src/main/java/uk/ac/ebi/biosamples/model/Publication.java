package uk.ac.ebi.biosamples.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Publication implements Comparable<Publication> {

	private String doi;
	private String pubmed;
	
	private Publication(String name, String affiliation) {
		this.doi = name;
		this.pubmed = affiliation;
	}

	@JsonProperty("doi") 
	public String getDoi() {
		return this.doi;
	}
	

	@JsonProperty("pubmed_id") 
	public String getPubMed() {
		return this.pubmed;
	}
		

	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof Publication)) {
            return false;
        }
        Publication other = (Publication) o;
        return Objects.equals(this.doi, other.doi) 
        		&& Objects.equals(this.pubmed, other.pubmed);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(doi, pubmed);
    }

	@Override
	public int compareTo(Publication other) {
		if (other == null) {
			return 1;
		}

		if (this.doi == null && other.doi != null) {
			return -1;
		}
		if (this.doi != null && other.doi == null) {
			return 1;
		}
		if (this.doi != null && other.doi != null 
				&& !this.doi.equals(other.doi)) {
			return this.doi.compareTo(other.doi);
		}

		if (this.pubmed == null && other.pubmed != null) {
			return -1;
		}
		if (this.pubmed != null && other.pubmed == null) {
			return 1;
		}
		if (this.pubmed != null && other.pubmed != null 
				&& !this.pubmed.equals(other.pubmed)) {
			return this.pubmed.compareTo(other.pubmed);
		}
		//no differences, must be the same
		return 0;
	}
		
		
	@JsonCreator
	public static Publication build(@JsonProperty("doi") String doi, 
			@JsonProperty("pubmed_id") String pubmed) {
		return new Publication(doi, pubmed);
	}
}

