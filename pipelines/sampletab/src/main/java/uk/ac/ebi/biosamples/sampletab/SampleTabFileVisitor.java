package uk.ac.ebi.biosamples.sampletab;

import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.biosamples.utils.ThreadUtils;


public class SampleTabFileVisitor extends SimpleFileVisitor<Path> {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	private final ExecutorService executorService;
	private final Map<String, Future<Void>> futures;
	private final RestTemplate restTemplate;
	private final URI uri;
	
	public SampleTabFileVisitor(ExecutorService executorService, Map<String, Future<Void>> futures, RestTemplate restTemplate, URI uri) {
		this.executorService = executorService;
		this.futures = futures;
		this.restTemplate = restTemplate;
		this.uri = uri;
	}
		
	@Override
	public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttribute) {
		if (path.endsWith("sampletab.txt")) {
			path = path.toAbsolutePath();
			
			log.info("Found "+path);
			
			Callable<Void> task = new SampleTabCallable(path, restTemplate, uri);
			
			futures.put(path.toString(), executorService.submit(task));

			//limit number of pending futures
			try {
				ThreadUtils.checkFutures(futures, 1000);
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
		return FileVisitResult.CONTINUE;
	}
}
