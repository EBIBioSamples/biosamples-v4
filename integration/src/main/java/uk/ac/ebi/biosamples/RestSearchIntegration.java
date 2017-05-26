package uk.ac.ebi.biosamples;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;

@Component
public class RestSearchIntegration extends AbstractIntegration {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	public RestSearchIntegration(BioSamplesClient client) {
		super(client);
	}
	
	@Override
	protected void phaseOne() {	
	}
	
	@Override
	protected void phaseTwo() {
		List<Resource<Sample>> samples = new ArrayList<>();		
		for (Resource<Sample> sample : client.fetchSampleResourceAll()) {
			samples.add(sample);
		}
		
		if (samples.size() <= 0) {
			throw new RuntimeException("No search results found!");
		}	
	}
	
	@Override
	protected void phaseThree() {	
	}
	
	@Override
	protected void phaseFour() {	
	}
	
	@Override
	protected void phaseFive() {	
	}

}
