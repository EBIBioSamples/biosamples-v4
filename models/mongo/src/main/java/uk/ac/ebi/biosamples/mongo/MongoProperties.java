package uk.ac.ebi.biosamples.mongo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MongoProperties {

	@Value("${biosamples.mongo.sample.writeConcern:0}")
	private String sampleWriteConcern;

	@Value("${biosamples.mongo.submission.writeConcern:0}")
	private String submissionWriteConcern;

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

	public String getSampleWriteConcern() {
		return sampleWriteConcern;
	}

	public String getSubmissionWriteConcern() {
		return submissionWriteConcern;
	}
}
