package uk.ac.ebi.biosamples;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WebappProperties {

	@Value("${biosamples.accession.prefix:SAMEA}")
	private String accessionPrefix;

	@Value("${biosamples.accession.min:100000}")
	private long accessionMinimum;

	@Value("${biosamples.accession.queuesize:100}")
	private int accessionQueueSize;

	public String getAccessionPrefix() {
		return accessionPrefix;
	}
	
	public long getAccessionMinimum() {
		return accessionMinimum;
	}
	
	public int getAcessionQueueSize() {
		return accessionQueueSize;
	}

}
