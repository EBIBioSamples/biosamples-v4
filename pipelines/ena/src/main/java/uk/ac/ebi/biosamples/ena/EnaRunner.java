package uk.ac.ebi.biosamples.ena;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

@Component
public class EnaRunner implements ApplicationRunner {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private PipelinesProperties pipelinesProperties;

	@Autowired
	private EraProDao eraProDao;
	
	@Autowired
	private EnaCallableFactory enaCallableFactory;
	
	@Autowired
	private NcbiCallableFactory ncbiCallableFactory;

	private Map<String, Future<Void>> futures = new HashMap<>();
	
	@Override
	public void run(ApplicationArguments args) throws Exception {

		log.info("Processing ENA pipeline...");

		// date format is YYYY-mm-dd
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
		} else {
			toDate = LocalDate.parse("3000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
		}
		
		if (pipelinesProperties.getThreadCount() == 0) {
			EraRowCallbackHandler eraRowCallbackHandler = new EraRowCallbackHandler(null, enaCallableFactory, futures);
			eraProDao.doSampleCallback(fromDate, toDate, eraRowCallbackHandler);
			//now print a list of things that failed
			if (EnaCallable.failedQueue.size() > 0) {
				log.info("Failed accessions: "+String.join(", ", EnaCallable.failedQueue));
			}			
		} else {
		
			try (AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, false, 
					pipelinesProperties.getThreadCount(), pipelinesProperties.getThreadCountMax())) {
	
				EraRowCallbackHandler eraRowCallbackHandler = new EraRowCallbackHandler(executorService, enaCallableFactory, futures);
				eraProDao.doSampleCallback(fromDate, toDate, eraRowCallbackHandler);
				
				NcbiRowCallbackHandler ncbiRowCallbackHandler = new NcbiRowCallbackHandler(executorService, ncbiCallableFactory, futures);
				eraProDao.getNcbiCallback(fromDate, toDate, ncbiRowCallbackHandler);
				
				log.info("waiting for futures");
				// wait for anything to finish
				ThreadUtils.checkFutures(futures, 0);
			} finally {
				//now print a list of things that failed
				if (EnaCallable.failedQueue.size() > 0) {
					log.info("Failed accessions: "+String.join(", ", EnaCallable.failedQueue));
				}
			}
		}
		
		//now print a list of things that failed
		if (EnaCallable.failedQueue.size() > 0) {
			log.info("Failed accessions: "+String.join(", ", EnaCallable.failedQueue));
		}
	}
	
	
	private static class EraRowCallbackHandler implements RowCallbackHandler {

		private final AdaptiveThreadPoolExecutor executorService;
		private final EnaCallableFactory enaCallableFactory;
		private final Map<String, Future<Void>> futures;
		
		public EraRowCallbackHandler(AdaptiveThreadPoolExecutor executorService, 
				EnaCallableFactory enaCallableFactory, 
				Map<String, Future<Void>>futures) {
			this.executorService = executorService;
			this.enaCallableFactory = enaCallableFactory;
			this.futures = futures;
		}
		
		@Override
		public void processRow(ResultSet rs) throws SQLException {
			String sampleAccession = rs.getString("BIOSAMPLE_ID");
			
			Callable<Void> callable = enaCallableFactory.build(sampleAccession); 
			if (executorService == null) {
				try {
					callable.call();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else {
				futures.put(sampleAccession, executorService.submit(callable));
				try {
					ThreadUtils.checkFutures(futures, 100);
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
	}
	
	private static class NcbiRowCallbackHandler implements RowCallbackHandler {

		private final AdaptiveThreadPoolExecutor executorService;
		private final NcbiCallableFactory ncbiCallableFactory;
		private final Map<String, Future<Void>> futures;
		
		public NcbiRowCallbackHandler(AdaptiveThreadPoolExecutor executorService,
				NcbiCallableFactory ncbiCallableFactory,
				Map<String, Future<Void>> futures) {
			this.executorService = executorService;
			this.ncbiCallableFactory = ncbiCallableFactory;
			this.futures = futures;
		}
		
		@Override
		public void processRow(ResultSet rs) throws SQLException {
			String sampleAccession = rs.getString("BIOSAMPLE_ID");
			
			Callable<Void> callable = ncbiCallableFactory.build(sampleAccession); 
			if (executorService == null) {
				try {
					callable.call();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else {
				futures.put(sampleAccession, executorService.submit(callable));
				try {
					ThreadUtils.checkFutures(futures, 100);
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
	}
	
}
