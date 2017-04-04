package uk.ac.ebi.biosamples.solr;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.ac.ebi.biosamples.messages.threaded.MessageBuffer;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

@Configuration
public class AgentSolrConfiguration {
	
	@Bean("SolrSampleMessageBuffer")
	public MessageBuffer<SolrSample> getSampleMessageBuffer(SolrSampleRepository neoSampleRepository) {
		return new MessageBuffer<>(neoSampleRepository);
	}
}
