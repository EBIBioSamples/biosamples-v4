package uk.ac.ebi.biosamples;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Sample;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageContent {
	
	private final Sample sample;
	private final CurationLink curationLink;
	public final boolean delete;

	private MessageContent(Sample sample, CurationLink curationLink, boolean delete) {
		this.sample = sample;
		this.curationLink = curationLink;
		this.delete = delete;
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
	
	
	public CurationLink getCurationLink() {
		return curationLink;
	}

	public static MessageContent build(Sample sample, boolean delete) {
		return new MessageContent(sample,  null, delete);
	}	
	public static MessageContent build(CurationLink curationLink, boolean delete) {
		return new MessageContent(null,  curationLink, delete);
	}
	@JsonCreator
	public static MessageContent build(@JsonProperty("sample") Sample sample, 
			@JsonProperty("curationLink") CurationLink curationLink, @JsonProperty("delete") boolean delete) {
		return new MessageContent(sample, curationLink, delete);
	}
}
