package uk.ac.ebi.biosamples.ena;

import uk.ac.ebi.biosamples.model.Sample;

/**
 * Bean to store everything fetched from ERAPRO for a {@link Sample}
 * 
 * @author dgupta
 */
public class SampleDBBean {
	private String sampleXml;
	private String firstPublic;
	private String lastUpdate;
	private String firstCreated;
	private int status;

	public String getSampleXml() {
		return sampleXml;
	}

	public void setSampleXml(String sampleXml) {
		this.sampleXml = sampleXml;
	}

	public String getFirstPublic() {
		return firstPublic;
	}

	public void setFirstPublic(String firstPublic) {
		this.firstPublic = firstPublic;
	}

	public String getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(String lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public String getFirstCreated() {
		return firstCreated;
	}

	public void setFirstCreated(String firstCreated) {
		this.firstCreated = firstCreated;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
}
