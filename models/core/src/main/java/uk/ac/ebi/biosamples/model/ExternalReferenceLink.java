package uk.ac.ebi.biosamples.model;

public class ExternalReferenceLink {

	private String sample;
	private String externalReference;
	
	private ExternalReferenceLink(String sample, String externalReference) {
		this.sample = sample;
		this.externalReference = externalReference;
	}
	
	static public ExternalReferenceLink build(String sample, String externalReference) {
		return new ExternalReferenceLink(sample, externalReference);
	}
}
