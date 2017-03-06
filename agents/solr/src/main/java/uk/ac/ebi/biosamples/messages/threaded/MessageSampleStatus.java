package uk.ac.ebi.biosamples.messages.threaded;

import java.util.concurrent.atomic.AtomicBoolean;

import uk.ac.ebi.biosamples.solr.model.SolrSample;

public class MessageSampleStatus {

	public final AtomicBoolean storedInSolr = new AtomicBoolean(false);
	
	public final SolrSample sample;
	
	private MessageSampleStatus(SolrSample sample) {
		this.sample = sample;
	}
	
	public static MessageSampleStatus build(SolrSample sample) {
		if (sample == null) throw new IllegalArgumentException("SolrSample sample cannot be null");
		return new MessageSampleStatus(sample);
	}
	
	
	
}
