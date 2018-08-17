package uk.ac.ebi.biosamples.ena;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Component
@ConditionalOnProperty(prefix = "job.autorun", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EnaRunner implements ApplicationRunner {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private PipelinesProperties pipelinesProperties;

	@Autowired
	private EraProDao eraProDao;
	
	@Autowired
	private EnaCallableFactory enaCallableFactory;
	
	@Autowired
	private NcbiCurationCallableFactory ncbiCallableFactory;

	private Map<String, Future<Void>> futures = new LinkedHashMap<>();
	
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
			
			NcbiRowCallbackHandler ncbiRowCallbackHandler = new NcbiRowCallbackHandler(null, ncbiCallableFactory, futures);
			eraProDao.getNcbiCallback(fromDate, toDate, ncbiRowCallbackHandler);
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
			} 
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
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else {
				futures.put(sampleAccession, executorService.submit(callable));
				try {
					ThreadUtils.checkFutures(futures, 100);
				} catch (RuntimeException e) {
					throw e;
				} catch (ExecutionException e) {
					throw new RuntimeException(e.getCause());
				}catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
	}
	
	private static class NcbiRowCallbackHandler implements RowCallbackHandler {

		private final AdaptiveThreadPoolExecutor executorService;
		private final NcbiCurationCallableFactory ncbiCallableFactory;
		private final Map<String, Future<Void>> futures;

		private Logger log = LoggerFactory.getLogger(getClass());
		
		public NcbiRowCallbackHandler(AdaptiveThreadPoolExecutor executorService,
				NcbiCurationCallableFactory ncbiCallableFactory,
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
				} catch (HttpClientErrorException e) {
					log.error("HTTP Client error body : "+e.getResponseBodyAsString());
					throw e;
				} catch (RuntimeException e) {
					throw e;
				} catch (ExecutionException e) {
					throw new RuntimeException(e.getCause());
				}catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
	}
	
}
