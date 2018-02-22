package uk.ac.ebi.biosamples.legacyxml;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

@Component
public class ClientImporter implements ApplicationRunner {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	private final BioSamplesClient client;
	private final ObjectMapper objectMapper;
	
	public ClientImporter(BioSamplesClient client, 
			ObjectMapper objectMapper) {
		this.client = client;
		this.objectMapper = objectMapper;
	}
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		if (!"clientimport".equals(args.getNonOptionArgs().get(0))) {
			return;
		}

		Path inputJsonPath = Paths.get(args.getNonOptionArgs().get(1));
	    Map<String, Future<Resource<Sample>>> futures = new HashMap<>();

		try (InputStream inputStream = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(inputJsonPath)))) {

		    JsonParser parser = objectMapper.getFactory().createParser(inputStream);
		    if (parser.nextToken() != JsonToken.START_ARRAY) {
		    	throw new IllegalStateException("A JSON array was expected");
			}
		    while (parser.nextToken() == JsonToken.START_ARRAY) {
		    	log.info("skipping start array");
		    }
		    Iterator<Sample> it = objectMapper.readerFor(Sample.class).readValues(parser);
			while (it.hasNext()) {
				Sample sample = it.next();
				Future<Resource<Sample>> future = client.persistSampleResourceAsync(sample);
				futures.put(sample.getAccession(), future);
				ThreadUtils.checkFutures(futures, 1000);
			}
		} finally {
			ThreadUtils.checkFutures(futures, 0);
		}
	}
}
