package uk.ac.ebi.biosamples.sampletab;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;

public class SampleTabCallable implements Callable<Void> {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final Path path;
	private final RestTemplate restTemplate;
	private final URI uri;

	public static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<String>();

	public SampleTabCallable(Path path, RestTemplate restTemplate, URI uri) {
		this.path = path;
		this.restTemplate = restTemplate;
		this.uri = uri;
	}

	@Override
	public Void call() throws Exception {

		try {

			String content = new String(Files.readAllBytes(path));

			RequestEntity<?> request = RequestEntity.post(uri).contentType(MediaType.TEXT_PLAIN).body(content);

			restTemplate.exchange(request, new ParameterizedTypeReference<String>() {
			});
		} catch (Exception e) {
			log.error("Error handling "+path.toString(), e);
			failedQueue.add(path.toString());
			if (failedQueue.size() > 100) {
				log.error("More than 100 failures!");
				throw e;
			}
		}

		return null;

	}
}
