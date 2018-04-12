package uk.ac.ebi.biosamples.export;

import java.io.File;
import java.io.FileWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;

@Component
public class ExportRunner implements ApplicationRunner {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final BioSamplesClient bioSamplesClient;	
	private final ObjectMapper objectMapper;
	
	public ExportRunner(BioSamplesClient bioSamplesClient, ObjectMapper objectMapper) {
		this.bioSamplesClient = bioSamplesClient;
		this.objectMapper = objectMapper;
	}
	

	@Override
	public void run(ApplicationArguments args) throws Exception {
		String jsonSampleFilename = args.getNonOptionArgs().get(1);
		long oldTime = System.nanoTime();		
		try {
			boolean first = true;
			try (
				FileWriter jsonSampleWriter = new FileWriter(new File(jsonSampleFilename));
			) {
				jsonSampleWriter.write("[\n");
				for (Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll()) {
					log.trace("Handling "+sampleResource);
					Sample sample = sampleResource.getContent();
					if (sample == null) {
						throw new RuntimeException("Sample should not be null");
					}
					if (!first) {
						jsonSampleWriter.write(",\n");
					}
					jsonSampleWriter.write(objectMapper.writeValueAsString(sample));
					first = false;
				}
				jsonSampleWriter.write("\n]");
			} finally {
			}
		} finally {			
		}
		long elapsed = System.nanoTime()-oldTime;
		log.info("Exported samples from in "+(elapsed/1000000000l)+"s");
	}
	
}
