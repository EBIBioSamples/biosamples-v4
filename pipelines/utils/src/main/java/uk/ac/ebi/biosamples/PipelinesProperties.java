package uk.ac.ebi.biosamples;

import java.io.File;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PipelinesProperties {
	
	@Value("${biosamples.ncbi.file:biosample_set.xml.gz}")
	private File ncbiFile;

	@Value("${biosamples.threadcount:1}")
	private int threadCount;

	@Value("${biosamples.threadcount.max:32}")
	private int threadCountMax;
	
	public File getNcbiFile() {
		return ncbiFile;
	}

	public int getThreadCount() {
		return threadCount;
	}

	public int getThreadCountMax() {
		return threadCountMax;
	}
}
