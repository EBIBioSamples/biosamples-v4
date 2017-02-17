package uk.ac.ebi.biosamples;

import java.io.File;
import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PipelinesProperties {
	
	@Value("${biosamples.ncbi.file:biosample_set.xml.gz}")
	private File ncbiFile;

	@Value("${biosamples.ncbi.threadcount:1}")
	private int ncbiThreadCount;

	@Value("${biosamples.ena.threadcount:1}")
	private int enaThreadCount;
	
	@Value("${biosamples.submissionuri:http://localhost:8081}")
	private URI biosampleSubmissionURI;

	public URI getBiosampleSubmissionURI() {
		return biosampleSubmissionURI;
	}

	public File getNcbiFile() {
		return ncbiFile;
	}

	public int getNcbiThreadCount() {
		return ncbiThreadCount;
	}

	public int getEnaThreadCount() {
		return ncbiThreadCount;
	}

}
