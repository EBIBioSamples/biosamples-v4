package uk.ac.ebi.biosamples.ncbi;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.XMLFragmenter;

@Component
public class NCBI implements ApplicationRunner {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Value("${biosamples.pipelines.ncbi.threadcount:1}")
	private int threadCount;
	
	@Autowired
	private NCBIFTP ncbiftp;
	
	@Autowired
	private XMLFragmenter xmlFragmenter;
	
	@Autowired
	private NCBIFragmentCallback callback;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		//only do things if we are told to
		if (!args.containsOption("ncbi")) {
			log.info("skipping ncbi");
			return;	
		}
		
		log.info("Processing NCBI pipeline...");

		LocalDate fromDate = null;
		if (args.getOptionNames().contains("from")) {
			fromDate = LocalDate.parse(args.getOptionValues("from").iterator().next(), DateTimeFormatter.ISO_LOCAL_DATE);
		}
		LocalDate toDate = null;
		if (args.getOptionNames().contains("until")) {
			toDate = LocalDate.parse(args.getOptionValues("until").iterator().next(), DateTimeFormatter.ISO_LOCAL_DATE);
		}

		callback.setFromDate(fromDate);
		callback.setToDate(toDate);
		
		
		//TODO read this from arguments
		ExecutorService executorService = null;
		if (threadCount > 0) {
			//executorService = Executors.newFixedThreadPool(threadCount);
			executorService = AdaptiveThreadPoolExecutor.create();
			try {
				Queue<Future<Void>> futures = new LinkedList<>();
				
				callback.setExecutorService(executorService);
				callback.setFutures(futures);
				
				//this does the actual processing
				try (InputStream is = ncbiftp.streamFromLocalCopy()) {
					xmlFragmenter.handleStream(is, "UTF-8", callback);
				}
				
				log.info("waiting for futures");
				
				//wait for anything to finish
				for (Future<Void> future : futures) {
					future.get();
				}
			} finally {
				executorService.shutdown();
				executorService.awaitTermination(1, TimeUnit.MINUTES);
			}
		} else {
			//do all on master thread
			//this does the actual processing
			try (InputStream is = ncbiftp.streamFromLocalCopy()) {
				xmlFragmenter.handleStream(is, "UTF-8", callback);
			}
		}
		 
		log.info("Processed NCBI pipeline");
	}

}
