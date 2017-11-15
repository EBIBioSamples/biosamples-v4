package uk.ac.ebi.biosamples.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Publication implements Comparable<Publication> {

	private String doi;
	private String pubmed_id;
	
	private Publication(String doi, String pubmed_id) {
		this.doi = doi;
		this.pubmed_id = pubmed_id;
	}

	@JsonProperty("doi") 
	public String getDoi() {
		return this.doi;
	}
	

	@JsonProperty("pubmed_id") 
	public String getPubMedId() {
		return this.pubmed_id;
	}
		

	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof Publication)) {
            return false;
        }
        Publication other = (Publication) o;
        return Objects.equals(this.doi, other.doi) 
        		&& Objects.equals(this.pubmed_id, other.pubmed_id);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(doi, pubmed_id);
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

		if (this.pubmed_id == null && other.pubmed_id != null) {
			return -1;
		}
		if (this.pubmed_id != null && other.pubmed_id == null) {
			return 1;
		}
		if (this.pubmed_id != null && other.pubmed_id != null 
				&& !this.pubmed_id.equals(other.pubmed_id)) {
			return this.pubmed_id.compareTo(other.pubmed_id);
		}
		//no differences, must be the same
		return 0;
	}
		
		
	@JsonCreator
	public static Publication build(@JsonProperty("doi") String doi, 
			@JsonProperty("pubmed_id") String pubmedId) {
		return new Publication(doi, pubmedId);
	}
}

