package uk.ac.ebi.biosamples.export;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.MailSender;

@Component
public class ExportRunner implements ApplicationRunner {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final BioSamplesClient bioSamplesClient;	
	private final ObjectMapper objectMapper;
	
	public ExportRunner(BioSamplesClient bioSamplesClient, ObjectMapper objectMapper) {
		//ensure the client is public
		if (bioSamplesClient.getPublicClient().isPresent()) {
			this.bioSamplesClient = bioSamplesClient.getPublicClient().get();
		} else {
			this.bioSamplesClient = bioSamplesClient;
		}
		this.objectMapper = objectMapper;
	}
	

	@Override
	public void run(ApplicationArguments args) throws Exception {
		String jsonSampleFilename = args.getNonOptionArgs().get(0);
		long oldTime = System.nanoTime();	
		int sampleCount = 0;
		boolean isPassed = true;
		try {
			boolean first = true;
			try (
				Writer jsonSampleWriter = args.getOptionValues("gzip") == null 
					? new OutputStreamWriter(new FileOutputStream(jsonSampleFilename), "UTF-8")
					: new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(jsonSampleFilename)), "UTF-8");	
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
					sampleCount += 1;
				}
				jsonSampleWriter.write("\n]");
			} finally {
			}
		} catch (final Exception e) {
			isPassed = false;
		}
		finally {
			MailSender.sendEmail("Export", null, isPassed);
			long elapsed = System.nanoTime()-oldTime;
			log.info("Exported "+sampleCount+" samples in "+(elapsed/1000000000l)+"s");
		}
	}
}
