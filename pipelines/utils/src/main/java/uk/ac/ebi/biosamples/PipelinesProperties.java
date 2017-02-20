package uk.ac.ebi.biosamples;

import java.io.File;
import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PipelinesProperties {
	
	@Value("${biosamples.ncbi.file:biosample_set.xml.gz}")
	private File ncbiFile;

	@Value("${biosamples.threadcount:1}")
	private int threadCount;
	
	@Value("${biosamples.submissionuri:http://localhost:8081}")
	private URI biosampleSubmissionURI;

	public URI getBiosampleSubmissionURI() {
		return biosampleSubmissionURI;
	}

	public File getNcbiFile() {
		return ncbiFile;
	}

	public int getThreadCount() {
		return threadCount;
	}
}
