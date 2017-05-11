package uk.ac.ebi.biosamples.solr;

import java.util.Collection;

import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.messages.threaded.MessageBuffer;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

@Component
public class SolrMessageBuffer extends MessageBuffer<SolrSample, SolrSampleRepository> {

	public SolrMessageBuffer(SolrSampleRepository repository, AgentSolrProperties properties) {
		super(repository, properties.getAgentSolrQueueSize(), properties.getAgentSolrQueueTime());
	}

	@Override
	protected void save(SolrSampleRepository repository, Collection<SolrSample> samples) {
		repository.save(samples);		
	}

}
