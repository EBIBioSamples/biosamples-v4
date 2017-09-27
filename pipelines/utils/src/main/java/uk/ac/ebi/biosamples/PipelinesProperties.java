package uk.ac.ebi.biosamples;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PipelinesProperties {
	
	@Value("${biosamples.ncbi.file:biosample_set.xml.gz}")
	private String ncbiFile;

	@Value("${biosamples.threadcount:1}")
	private int threadCount;

	@Value("${biosamples.threadcount.max:32}")
	private int threadCountMax;

	@Value("${biosamples.connectioncount.max:32}")
	private int connectionCountMax;
	
	@Value("${biosamples.connectioncount.default:32}")
	private int connectionCountDefault;
	
	@Value("${biosamples.connectioncount.ols:32}")
	private int connectionCountOls;
	
	@Value("${biosamples.connectioncount.zooma:32}")
	private int connectionCountZooma;
	
	@Value("${biosamples.zooma:http://wwwdev.ebi.ac.uk/spot/zooma}")
	private String zooma;
	
	@Value("${biosamples.ols:http://wwwdev.ebi.ac.uk/ols}")
	private String ols;
	
	@Value("${biosamples.ncbi.domain:self.BiosampleImportNCBI}")
	private String ncbiDomain;
	
	@Value("${biosamples.ena.domain:self.BiosampleImportENA}")
	private String enaDomain;
	
	public String getNcbiFile() {
		return ncbiFile;
	}
	public String getNcbiDomain() {
		return ncbiDomain;
	}
	public String getEnaDomain() {
		return enaDomain;
	}

	public int getThreadCount() {
		return threadCount;
	}

	public int getThreadCountMax() {
		return threadCountMax;
	}

	public int getConnectionCountMax() {
		return connectionCountMax;
	}

	public int getConnectionCountDefault() {
		return connectionCountDefault;
	}

	public int getConnectionCountOls() {
		return connectionCountOls;
	}

	public int getConnectionCountZooma() {
		return connectionCountZooma;
	}
	
	public String getZooma() {
		return zooma;
	}
	
	public String getOls() {
		return ols;
	}
}
