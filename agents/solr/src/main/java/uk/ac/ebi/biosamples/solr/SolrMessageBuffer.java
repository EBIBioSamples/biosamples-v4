package uk.ac.ebi.biosamples.solr;

import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.messages.threaded.MessageBuffer;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

import java.util.Collection;

@Component
public class SolrMessageBuffer extends MessageBuffer<String, SolrSample> {

	private final SolrSampleRepository repository;
	
	public SolrMessageBuffer(BioSamplesProperties properties, SolrSampleRepository repository) {
		super(properties.getAgentSolrQueueSize(), properties.getAgentSolrQueueTime());
		this.repository = repository;
	}

	@Override
	public void save(Collection<SolrSample> samples) {
		repository.save(samples);		
	}

}
