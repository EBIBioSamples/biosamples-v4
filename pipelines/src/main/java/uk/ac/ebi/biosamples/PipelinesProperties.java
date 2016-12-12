package uk.ac.ebi.biosamples;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PipelinesProperties {
	
	@Value("${biosamples.ncbi.httpuri:http://ftp.ncbi.nlm.nih.gov/biosample/biosample_set.xml.gz}")
	private URI ncbiHttpUri;

	@Value("${biosamples.ncbi.threadcount:1}")
	private int ncbiThreadCount;
	
	@Value("${biosamples.submissionuri:http://localhost:8081/}")
	private URI biosampleSubmissionURI;

	public URI getBiosampleSubmissionURI() {
		return biosampleSubmissionURI;
	}

	public URI getNCBIHttpUri() {
		return ncbiHttpUri;
	}

	public int getNCBIThreadCount() {
		return ncbiThreadCount;
	}

}
