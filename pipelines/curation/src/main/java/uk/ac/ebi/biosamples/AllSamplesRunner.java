package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.zooma.ZoomaProcessor;

@Component
public class AllSamplesRunner implements ApplicationRunner {


	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private BioSamplesClient biosamplesClient;
	
	@Autowired
	private ZoomaProcessor zoomaProcessor;
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		
		for (Resource<Sample> sampleResource : biosamplesClient.fetchSampleResourceAll()) {
			log.info("Processing "+sampleResource.getContent().getAccession());
			
			zoomaProcessor.process(sampleResource);
		}
	}

}
