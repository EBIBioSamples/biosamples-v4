package uk.ac.ebi.biosamples.ena;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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

@Component
@ConditionalOnProperty(prefix = "job.autorun", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EnaRunner implements ApplicationRunner {
	private final static Logger log = LoggerFactory.getLogger(EnaRunner.class);

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
		boolean suppressionRunner = true;

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

		if (args.getOptionNames().contains("suppressionRunner")) {
			if (args.getOptionValues("suppressionRunner").iterator().next().equalsIgnoreCase("false")) {
				suppressionRunner = false;
			}
		}

		log.info("Suppression Runner is to be executed: " + suppressionRunner);

		importEraSamples(fromDate, toDate);

		if (suppressionRunner) {
			// handler for suppressed ENA samples
			handleSuppressedEnaSamples();
			// handler for suppressed NCBI/DDBJ samples - using separate
			// AdaptiveThreadPoolExecutor for not putting too much load on the ThreadPool
			handleSuppressedNcbiDdbjSamples();
		}
	}

	private void importEraSamples(LocalDate fromDate, LocalDate toDate)
			throws InterruptedException, ExecutionException, Exception {
		if (pipelinesProperties.getThreadCount() == 0) {
			EraRowCallbackHandler eraRowCallbackHandler = new EraRowCallbackHandler(null, enaCallableFactory, futures);
			eraProDao.doSampleCallback(fromDate, toDate, eraRowCallbackHandler);

			NcbiRowCallbackHandler ncbiRowCallbackHandler = new NcbiRowCallbackHandler(null, ncbiCallableFactory,
					futures);
			eraProDao.getNcbiCallback(fromDate, toDate, ncbiRowCallbackHandler);
		} else {

			try (AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, false,
					pipelinesProperties.getThreadCount(), pipelinesProperties.getThreadCountMax())) {

				EraRowCallbackHandler eraRowCallbackHandler = new EraRowCallbackHandler(executorService,
						enaCallableFactory, futures);
				eraProDao.doSampleCallback(fromDate, toDate, eraRowCallbackHandler);

				NcbiRowCallbackHandler ncbiRowCallbackHandler = new NcbiRowCallbackHandler(executorService,
						ncbiCallableFactory, futures);
				eraProDao.getNcbiCallback(fromDate, toDate, ncbiRowCallbackHandler);

				log.info("waiting for futures"); // wait for anything to finish
				ThreadUtils.checkFutures(futures, 0);
			}
		}
	}

	/**
	 * Handler for suppressed ENA samples. If status of sample is different in
	 * BioSamples, status will be updated so SUPPRESSED. If sample doesn't exist it
	 * will be created
	 * 
	 * @throws Exception in case of failures
	 */
	private void handleSuppressedEnaSamples() throws Exception {
		log.info("Fetching all suppressed ENA samples. "
				+ "If they exist in BioSamples with different status, their status will be updated. If the sample don't exist at all it will be POSTed to BioSamples client");

		try (final AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, false,
				pipelinesProperties.getThreadCount(), pipelinesProperties.getThreadCountMax())) {

			final EnaSuppressedSamplesCallbackHandler enaSuppressedSamplesCallbackHandler = new EnaSuppressedSamplesCallbackHandler(
					executorService, enaCallableFactory, futures);
			eraProDao.doGetSuppressedEnaSamples(enaSuppressedSamplesCallbackHandler);
		}
	}

	/**
	 * Handler for suppressed NCBI/DDBJ samples. If status of sample is different in
	 * BioSamples, status will be updated to SUPPRESSED
	 * 
	 * @throws Exception in case of failures
	 */
	private void handleSuppressedNcbiDdbjSamples() throws Exception {
		log.info("Fetching all suppressed NCBI/DDBJ samples. "
				+ "If they exist in BioSamples with different status, their status will be updated. If the sample don't exist at all it will be POSTed to BioSamples client");

		try (final AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, false,
				pipelinesProperties.getThreadCount(), pipelinesProperties.getThreadCountMax())) {

			final NcbiDdbjSuppressedSamplesCallbackHandler ncbiDdbjSuppressedSamplesCallbackHandler = new NcbiDdbjSuppressedSamplesCallbackHandler(
					executorService, ncbiCallableFactory, futures);
			eraProDao.doGetSuppressedNcbiDdbjSamples(ncbiDdbjSuppressedSamplesCallbackHandler);
		}
	}

	/**
	 * @author dgupta
	 * 
	 *         {@link RowCallbackHandler} for suppressed ENA samples
	 */
	private static class EnaSuppressedSamplesCallbackHandler implements RowCallbackHandler {
		private final AdaptiveThreadPoolExecutor executorService;
		private final EnaCallableFactory enaCallableFactory;
		private final Map<String, Future<Void>> futures;

		public EnaSuppressedSamplesCallbackHandler(final AdaptiveThreadPoolExecutor executorService,
				final EnaCallableFactory enaCallableFactory, final Map<String, Future<Void>> futures) {
			this.executorService = executorService;
			this.enaCallableFactory = enaCallableFactory;
			this.futures = futures;
		}

		@Override
		public void processRow(ResultSet rs) throws SQLException {
			final String sampleAccession = rs.getString("BIOSAMPLE_ID");
			final boolean suppressionHandler = true;

			Callable<Void> callable = enaCallableFactory.build(sampleAccession, suppressionHandler);
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
			}
		}
	}

	/**
	 * @author dgupta
	 * 
	 *         {@link RowCallbackHandler} for suppressed NCBI/DDBJ samples
	 */
	private static class NcbiDdbjSuppressedSamplesCallbackHandler implements RowCallbackHandler {
		private final AdaptiveThreadPoolExecutor executorService;
		private final NcbiCurationCallableFactory ncbiCurationCallableFactory;
		private final Map<String, Future<Void>> futures;

		public NcbiDdbjSuppressedSamplesCallbackHandler(final AdaptiveThreadPoolExecutor executorService,
				final NcbiCurationCallableFactory ncbiCurationCallableFactory,
				final Map<String, Future<Void>> futures) {
			this.executorService = executorService;
			this.ncbiCurationCallableFactory = ncbiCurationCallableFactory;
			this.futures = futures;
		}

		@Override
		public void processRow(ResultSet rs) throws SQLException {
			final String sampleAccession = rs.getString("BIOSAMPLE_ID");
			final boolean suppressionHandler = true;

			Callable<Void> callable = ncbiCurationCallableFactory.build(sampleAccession, suppressionHandler);
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
			}
		}
	}

	private static class EraRowCallbackHandler implements RowCallbackHandler {

		private final AdaptiveThreadPoolExecutor executorService;
		private final EnaCallableFactory enaCallableFactory;
		private final Map<String, Future<Void>> futures;

		public EraRowCallbackHandler(AdaptiveThreadPoolExecutor executorService, EnaCallableFactory enaCallableFactory,
				Map<String, Future<Void>> futures) {
			this.executorService = executorService;
			this.enaCallableFactory = enaCallableFactory;
			this.futures = futures;
		}

		private enum ENAStatus {
			PUBLIC(4), SUPPRESSED(5), KILLED(6), TEMPORARY_SUPPRESSED(7), TEMPORARY_KILLED(8);

			private int value;
			private static Map<Integer, ENAStatus> map = new HashMap<>();

			ENAStatus(int value) {
				this.value = value;
			}

			static {
				for (ENAStatus enaStatus : ENAStatus.values()) {
					map.put(enaStatus.value, enaStatus);
				}
			}

			public static ENAStatus valueOf(int pageType) {
				return (ENAStatus) map.get(pageType);
			}
		}

		@Override
		public void processRow(ResultSet rs) throws SQLException {
			String sampleAccession = rs.getString("BIOSAMPLE_ID");
			int statusID = rs.getInt("STATUS_ID");
			ENAStatus enaStatus = ENAStatus.valueOf(statusID);
			switch (enaStatus) {
			case PUBLIC:
			case SUPPRESSED:
			case TEMPORARY_SUPPRESSED:
				log.info(String.format("%s is being handled as status is %s", sampleAccession, enaStatus.name()));
				// update if sample already exists else import
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
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
				break;
			case KILLED:
			case TEMPORARY_KILLED:
				// TODO
				// set hold date
				// update if sample already exists else ignore
				log.info(String.format("%s would be handled as status is %s", sampleAccession, enaStatus.name()));
				break;
			default:
				log.info(String.format("%s would be ignored  as status is %s", sampleAccession, enaStatus.name()));
			}
		}

	}

	private static class NcbiRowCallbackHandler implements RowCallbackHandler {

		private final AdaptiveThreadPoolExecutor executorService;
		private final NcbiCurationCallableFactory ncbiCallableFactory;
		private final Map<String, Future<Void>> futures;

		private Logger log = LoggerFactory.getLogger(getClass());

		public NcbiRowCallbackHandler(AdaptiveThreadPoolExecutor executorService,
				NcbiCurationCallableFactory ncbiCallableFactory, Map<String, Future<Void>> futures) {
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
					log.error("HTTP Client error body : " + e.getResponseBodyAsString());
					throw e;
				} catch (RuntimeException e) {
					throw e;
				} catch (ExecutionException e) {
					throw new RuntimeException(e.getCause());
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}

	}
}
