package uk.ac.ebi.biosamples.solr;

import java.util.Collection;

import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.messages.threaded.MessageBuffer;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

@Component
public class SolrMessageBuffer extends MessageBuffer<SolrSample> {

	private final SolrSampleRepository repository;
	
	public SolrMessageBuffer(AgentSolrProperties properties, SolrSampleRepository repository) {
		super(properties.getAgentSolrQueueSize(), properties.getAgentSolrQueueTime());
		this.repository = repository;
	}

	@Override
	protected void save(Collection<SolrSample> samples) {
		repository.save(samples);		
	}

}
