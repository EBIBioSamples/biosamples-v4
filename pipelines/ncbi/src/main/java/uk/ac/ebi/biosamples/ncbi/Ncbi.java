package uk.ac.ebi.biosamples.ncbi;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.ClientProperties;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.XmlFragmenter;

@Component
public class Ncbi implements ApplicationRunner {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private PipelinesProperties pipelinesProperties;

	@Autowired
	private XmlFragmenter xmlFragmenter;

	@Autowired
	private NcbiFragmentCallback callback;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Processing NCBI pipeline...");

		LocalDate fromDate = null;
		if (args.getOptionNames().contains("from")) {
			fromDate = LocalDate.parse(args.getOptionValues("from").iterator().next(),
					DateTimeFormatter.ISO_LOCAL_DATE);
		} else {
			fromDate = LocalDate.parse("1000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
		}
		LocalDate toDate = null;
		if (args.getOptionNames().contains("until")) {
			toDate = LocalDate.parse(args.getOptionValues("until").iterator().next(), DateTimeFormatter.ISO_LOCAL_DATE);
		}else {
			toDate = LocalDate.parse("3000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
		}

		callback.setFromDate(fromDate);
		callback.setToDate(toDate);

		try (InputStream is = new GZIPInputStream(new BufferedInputStream(new FileInputStream(pipelinesProperties.getNcbiFile())))) {

			if (pipelinesProperties.getThreadCount() > 0) {
				ExecutorService executorService = null;
				try {
					executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true, pipelinesProperties.getThreadCount());
					Queue<Future<Void>> futures = new LinkedList<>();

					callback.setExecutorService(executorService);
					callback.setFutures(futures);

					// this does the actual processing
					xmlFragmenter.handleStream(is, "UTF-8", callback);

					log.info("waiting for futures");

					// wait for anything to finish
					for (Future<Void> future : futures) {
						future.get();
					}
				} finally {
					log.info("shutting down");
					executorService.shutdown();
					executorService.awaitTermination(1, TimeUnit.MINUTES);
				}
			} else {
				// do all on master thread
				// this does the actual processing
				xmlFragmenter.handleStream(is, "UTF-8", callback);
			}
		}

		log.info("Processed NCBI pipeline");
	}

}
