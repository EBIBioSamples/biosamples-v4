package uk.ac.ebi.biosamples.sampletab;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
	private final Instant from;
	private final Instant until;
	
	public SampleTabFileVisitor(ExecutorService executorService, Map<String, Future<Void>> futures, 
			RestTemplate restTemplate, URI uri, LocalDate from, LocalDate until) {
		this.executorService = executorService;
		this.futures = futures;
		this.restTemplate = restTemplate;
		this.uri = uri;
		this.from = from.atStartOfDay().toInstant(ZoneOffset.UTC);
		this.until = until.atStartOfDay().toInstant(ZoneOffset.UTC);
	}
	
	@Override
	public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) {
		Path sampleTabFile = Paths.get(path.toString(), "sampletab.txt");
		//weird exists / not exists / can't tell things happening here
		//https://docs.oracle.com/javase/tutorial/essential/io/check.html
		if (Files.exists(sampleTabFile) && !Files.notExists(sampleTabFile)) {

			log.trace("Found "+path);
			
			Callable<Void> task = new SampleTabCallable(sampleTabFile, restTemplate, uri, from, until);
			
			futures.put(path.toString(), executorService.submit(task));

			//limit number of pending futures
			try {
				ThreadUtils.checkFutures(futures, 100);
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
			
			//don't bother visiting any further, found the file we need
			return FileVisitResult.SKIP_SUBTREE;
		}
		return FileVisitResult.CONTINUE;
	}
	
}
