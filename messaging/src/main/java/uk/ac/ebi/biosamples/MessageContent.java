package uk.ac.ebi.biosamples;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Sample;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageContent {
	
	private final Sample sample;
	private final CurationLink curationLink;
	private final List<Sample> related;
	public final boolean delete;
	private final long creationTime;
	

	private MessageContent(Sample sample, CurationLink curationLink, List<Sample> related, boolean delete, long creationTime) {
		this.sample = sample;
		this.curationLink = curationLink;
		this.related = related;
		this.delete = delete;
		this.creationTime = creationTime;
	}
	
	public boolean hasSample() {
		return this.sample != null;
	}	
	
	public boolean hasCurationLink() {
		return this.curationLink != null;
	}
	
	public Sample getSample() {
		return sample;
	}
	
	public List<Sample> getRelated() {
		return related;
	}

	public CurationLink getCurationLink() {
		return curationLink;
	}
	
	public long getCreationTime() {
		return creationTime;
	}
	
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("MessageContent(");
    	sb.append(sample);
    	sb.append(",");
    	sb.append(curationLink);
    	sb.append(",");
    	sb.append(related);
    	sb.append(",");
    	sb.append(delete);
    	sb.append(")");
    	return sb.toString();
    }

	@JsonCreator
	public static MessageContent build(@JsonProperty("sample") Sample sample, 
			@JsonProperty("curationLink") CurationLink curationLink, 
			@JsonProperty("related") List<Sample> related, @JsonProperty("delete") boolean delete) {
		long creationTime = System.nanoTime();
		return new MessageContent(sample, curationLink, related, delete, creationTime);
	}
}
