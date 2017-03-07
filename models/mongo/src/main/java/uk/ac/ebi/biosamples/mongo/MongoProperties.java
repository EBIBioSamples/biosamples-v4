package uk.ac.ebi.biosamples.mongo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MongoProperties {

	@Value("${biosamples.mongo.sample.writeConcern:0}")
	private String sampleWriteConcern;

	@Value("${biosamples.mongo.submission.writeConcern:0}")
	private String submissionWriteConcern;

	public String getSampleWriteConcern() {
		return sampleWriteConcern;
	}

	public String getSubmissionWriteConcern() {
		return submissionWriteConcern;
	}
}
