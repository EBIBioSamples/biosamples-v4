package uk.ac.ebi.biosamples.messages.threaded;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.util.Assert;

import uk.ac.ebi.biosamples.models.Sample;
import uk.ac.ebi.biosamples.solr.model.SolrSample;

public class MessageSampleStatus {

	public final AtomicBoolean storedInSolr = new AtomicBoolean(false);
	
	public final SolrSample sample;
	
	public MessageSampleStatus(SolrSample sample) {
		Assert.notNull(sample);
		
		this.sample = sample;
	}
	
	
	
}
