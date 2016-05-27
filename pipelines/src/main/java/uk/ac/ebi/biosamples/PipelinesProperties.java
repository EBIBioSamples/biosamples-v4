package uk.ac.ebi.biosamples;

import java.io.File;

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
	
	public String getNCBIFTPServer() {
		return ncbiFTPserver;
	}
	
	public File getNCBILocalFile() {
		return ncbiLocalFile.getAbsoluteFile();
	}
	
	public String getNCBIRemotePath() {
		return ncbiRemotePath;
	}

}
