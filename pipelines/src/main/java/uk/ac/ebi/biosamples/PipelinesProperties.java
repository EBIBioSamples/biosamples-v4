package uk.ac.ebi.biosamples;

import java.io.File;
import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PipelinesProperties {
	
	@Value("${biosamples.ncbi.server:ftp.ncbi.nlm.nih.gov}")
	private String ncbiFTPserver;
	
	@Value("${biosamples.ncbi.localfile:ncbi.xml.gz}")
	private File ncbiLocalFile;
	
	@Value("${biosamples.ncbi.remotepath:/biosample/biosample_set.xml.gz}")
	private String ncbiRemotePath;
	
	@Value("${biosamples.ncbi.httpuri:http://ftp.ncbi.nlm.nih.gov/biosample/biosample_set.xml.gz}")
	private URI ncbiHttpUri;

	@Value("${biosamples.ncbi.threadcount:0}")
	private int ncbiThreadCount;
	
	@Value("${biosamples.submissionuri:http://localhost:8083/}")
	private URI biosampleSubmissionURI;
	
	public String getNCBIFTPServer() {
		return ncbiFTPserver;
	}
	
	public File getNCBILocalFile() {
		return ncbiLocalFile.getAbsoluteFile();
	}
	
	public String getNCBIRemotePath() {
		return ncbiRemotePath;
	}

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
