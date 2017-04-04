package uk.ac.ebi.biosamples.neo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.ac.ebi.biosamples.messages.threaded.MessageBuffer;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;

@Configuration
public class AgentNeo4JConfiguration {

	@Bean("NeoSampleMessageBuffer")
	public MessageBuffer<NeoSample> getSampleMessageBuffer(NeoSampleRepository neoSampleRepository) {
		return new MessageBuffer<>(neoSampleRepository);
	}

}
