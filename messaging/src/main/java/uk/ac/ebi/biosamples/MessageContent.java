package uk.ac.ebi.biosamples;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.ExternalReferenceLink;
import uk.ac.ebi.biosamples.model.Sample;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageContent {
	
	private final Sample sample;
	private final ExternalReferenceLink externalReferenceLink;
	private final CurationLink curationLink;
	public final boolean delete;

	private MessageContent(Sample sample, ExternalReferenceLink externalReferenceLink, CurationLink curationLink, boolean delete) {
		this.sample = sample;
		this.externalReferenceLink = externalReferenceLink;
		this.curationLink = curationLink;
		this.delete = delete;
	}
	
	public boolean hasSample() {
		return this.sample != null;
	}
	
	public boolean hasExternalReferenceLink() {
		return this.externalReferenceLink != null;
	}
	
	public boolean hasCurationLink() {
		return this.curationLink != null;
	}
	
	public Sample getSample() {
		return sample;
	}
	
	public ExternalReferenceLink getExternalReferenceLink() {
		return externalReferenceLink;
	}
	
	public CurationLink getCurationLink() {
		return curationLink;
	}

	public static MessageContent build(Sample sample, boolean delete) {
		return new MessageContent(sample, null, null, delete);
	}	
	public static MessageContent build(ExternalReferenceLink externalReferenceLink, boolean delete) {
		return new MessageContent(null, externalReferenceLink, null, delete);
	}	
	public static MessageContent build(CurationLink curationLink, boolean delete) {
		return new MessageContent(null, null, curationLink, delete);
	}
	@JsonCreator
	public static MessageContent build(@JsonProperty("sample") Sample sample, 
			@JsonProperty("externalReferenceLink") ExternalReferenceLink externalReferenceLink, 
			@JsonProperty("curationLInk") CurationLink curationLink, @JsonProperty("delete") boolean delete) {
		return new MessageContent(sample, externalReferenceLink, curationLink, delete);
	}
}
