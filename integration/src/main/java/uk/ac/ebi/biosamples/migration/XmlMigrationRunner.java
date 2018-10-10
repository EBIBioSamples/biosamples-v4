package uk.ac.ebi.biosamples.migration;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.biosamples.service.XmlGroupToSampleConverter;
import uk.ac.ebi.biosamples.service.XmlSampleToSampleConverter;

//@Component
@Profile({"migration-xml"})
public class XmlMigrationRunner implements ApplicationRunner, ExitCodeGenerator {

	private final RestTemplate restTemplate;
	private ExecutorService executorService;

	private int exitCode = 1;
	private final Logger log = LoggerFactory.getLogger(getClass());

	@Value("${biosamples.migration.old:http://www.ebi.ac.uk/biosamples/xml/samples}")
	private String oldUrl;
	@Value("${biosamples.migration.new:http://www.ebi.ac.uk/biosamples/xml/samples}")
	private String newUrl;

	private final XmlSampleToSampleConverter xmlSampleToSampleConverter;
	private final XmlGroupToSampleConverter xmlGroupToSampleConverter;

	public XmlMigrationRunner(RestTemplateBuilder restTemplateBuilder, XmlSampleToSampleConverter xmlSampleToSampleConverter,
			XmlGroupToSampleConverter xmlGroupToSampleConverter) {
		restTemplate = restTemplateBuilder.build();
		this.xmlSampleToSampleConverter = xmlSampleToSampleConverter;
		this.xmlGroupToSampleConverter = xmlGroupToSampleConverter;
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Starting MigrationRunner");

		//use a keep alive strategy to try to make it easier to maintain connections for reuse
		ConnectionKeepAliveStrategy keepAliveStrategy = new ConnectionKeepAliveStrategy() {
		    public long getKeepAliveDuration(HttpResponse response, HttpContext context) {

		    	//check if there is a non-standard keep alive header present
		        HeaderElementIterator it = new BasicHeaderElementIterator
		            (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
		        while (it.hasNext()) {
		            HeaderElement he = it.nextElement();
		            String param = he.getName();
		            String value = he.getValue();
		            if (value != null && param.equalsIgnoreCase("timeout")) {
		                return Long.parseLong(value) * 1000;
		            }
		        }
		        //default to 15s if no header
		        return 15 * 1000;
		    }
		};

		//set a number of connections to use at once for multiple threads
		PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
		poolingHttpClientConnectionManager.setMaxTotal(32);
		poolingHttpClientConnectionManager.setDefaultMaxPerRoute(16);


		//make the actual client
		HttpClient httpClient = CachingHttpClientBuilder.create()
				.useSystemProperties()
				.setConnectionManager(poolingHttpClientConnectionManager)
				.setKeepAliveStrategy(keepAliveStrategy)
				.build();

		//and wire it into the resttemplate
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));

		//make sure there is a application/hal+json converter
		//traverson will make its own but not if we want to customize the resttemplate in any way (e.g. caching)
		List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new Jackson2HalModule());
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		MappingJackson2HttpMessageConverter halConverter = new TypeConstrainedMappingJackson2HttpMessageConverter(ResourceSupport.class);
		halConverter.setObjectMapper(mapper);
		halConverter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON));
		//make sure this is inserted first
		converters.add(0, halConverter);
		restTemplate.setMessageConverters(converters);





		try  {
			executorService = Executors.newFixedThreadPool(4);
			Queue<String> oldQueue = new ArrayBlockingQueue<>(1024);
			AtomicBoolean oldFinished = new AtomicBoolean(false);
			XmlAccessFetcherCallable oldCallable = new XmlAccessFetcherCallable(restTemplate, oldUrl, oldQueue, oldFinished, true);

			Queue<String> newQueue = new ArrayBlockingQueue<>(1024);
			AtomicBoolean newFinished = new AtomicBoolean(false);
			XmlAccessFetcherCallable newCallable = new XmlAccessFetcherCallable(restTemplate, newUrl, newQueue, newFinished, false);

			Queue<String> bothQueue = new ArrayBlockingQueue<>(1024);
			AtomicBoolean bothFinished = new AtomicBoolean(false);

			AccessionQueueBothCallable bothCallable = new AccessionQueueBothCallable(oldQueue, oldFinished,
					newQueue, newFinished, bothQueue, bothFinished);

			XmlAccessionComparisonCallable comparisonCallable = new XmlAccessionComparisonCallable(restTemplate,
					oldUrl, newUrl, bothQueue, bothFinished
					, xmlSampleToSampleConverter, xmlGroupToSampleConverter, args.containsOption("comparison"));

//			comparisonCallable.compare("SAMEA3683023");

			Future<Void> oldFuture = executorService.submit(oldCallable);
			Future<Void> newFuture = executorService.submit(newCallable);
			Future<Void> bothFuture = executorService.submit(bothCallable);
			Future<Void> comparisonFuture = executorService.submit(comparisonCallable);

			oldFuture.get();
			newFuture.get();
			bothFuture.get();
			comparisonFuture.get();
		} finally {
			executorService.shutdownNow();
		}

		exitCode = 0;
		log.info("Finished MigrationRunner");
	}
}
