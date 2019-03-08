package uk.ac.ebi.biosamples;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.service.AapClientService;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.AttributeValidator;
import uk.ac.ebi.biosamples.service.SampleValidator;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessControlContext;
import java.util.*;

public class testCLient1 {
    public static void main(String[] args) {
        System.out.println("hello");
        BioSamplesClient client = null;
        try {
            String clientUrl = "http://wwwdev.ebi.ac.uk/biosamples";
//            String clientUrl = "http://localhost:8090";
            URI uri = new URI(clientUrl);
            SampleValidator sampleValidator = new SampleValidator(new AttributeValidator());
            AapClientService aapClientService = null;
            BioSamplesProperties bioSamplesProperties = new BioSamplesProperties();


            FieldUtils.writeField(bioSamplesProperties, "biosamplesClientUri", uri, true);
            FieldUtils.writeField(bioSamplesProperties, "biosamplesClientPagesize", 10, true);
            FieldUtils.writeField(bioSamplesProperties, "biosamplesClientTimeout", 60000, true);
            FieldUtils.writeField(bioSamplesProperties, "connectionCountMax", 8, true);
            FieldUtils.writeField(bioSamplesProperties, "connectionCountDefault", 8, true);
            FieldUtils.writeField(bioSamplesProperties, "threadCount", 1, true);
            FieldUtils.writeField(bioSamplesProperties, "threadCountMax", 8, true);
            FieldUtils.writeField(bioSamplesProperties, "biosamplesClientAapUri", new URI("https://explore.api.aai.ebi.ac.uk/auth"), true);
//            FieldUtils.writeField(bioSamplesProperties, "biosamplesClientAapUsername", "isurul", true);
//            FieldUtils.writeField(bioSamplesProperties, "biosamplesClientAapPassword", "alchemIst9", true);
            FieldUtils.writeField(bioSamplesProperties, "biosamplesClientCacheMaxEntries", 0, true);
            FieldUtils.writeField(bioSamplesProperties, "biosamplesClientCacheMaxObjectSize", 1048576, true);


            RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
//            restTemplateBuilder.customizers(restTemplateCustomizer(bioSamplesProperties));
//
//
//            restTemplateBuilder.requestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
//            restTemplateBuilder.additionalInterceptors(interceptors);
//
//
//            List<HttpMessageConverter<?>> converters = new ArrayList<>();
//            ObjectMapper mapper = new ObjectMapper();
//            mapper.registerModule(new Jackson2HalModule());
//            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//            MappingJackson2HttpMessageConverter halConverter = new TypeConstrainedMappingJackson2HttpMessageConverter(ResourceSupport.class);
//            halConverter.setObjectMapper(mapper);
//            halConverter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON));
//            //make sure this is inserted first
//            converters.add(0, halConverter);
//            restTemplateBuilder.messageConverters(converters);

            RestTemplate restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
            List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
            interceptors.add(new LoggingRequestInterceptor());
            restTemplate.setInterceptors(interceptors);


            restTemplateBuilder = new RestTemplateBuilder();
            restTemplateBuilder.configure(restTemplate);

            client = new BioSamplesClient(uri, restTemplateBuilder,
                    sampleValidator, aapClientService, bioSamplesProperties);

            List<String> accessionList = new ArrayList<>();
            accessionList.add("SAMEA470888");
            accessionList.add("SAMEA2186845");

            PagedResources<Resource<Sample>> pagedResources = client.fetchPagedSampleResource("", 0, 5);
            System.out.println(pagedResources.getContent().size());

            for (Optional<Resource<Sample>> sampleResource : client.fetchSampleResourceAll(accessionList)) {
                Sample sample = sampleResource.get().getContent();
                System.out.println("from list: " + sample.getAccession());
            }

            Optional<Sample> sample = client.fetchSample("SAMEA470888");
            System.out.println(sample.get().getAccession());


            System.out.println("Trying fetchall");
            Iterable<Resource<Sample>> iter = client.fetchSampleResourceAll();
            Iterator iterator = iter.iterator();
            if (iterator.hasNext()) {
                System.out.println(iterator.next());
            }

            System.out.println("Trying fetchall(accession)");
            for (Resource<Sample> sampleResource : client.fetchSampleResourceAll("SAMEA470888")) {
                Sample sample1 = sampleResource.getContent();
                System.out.println("form text: " + sample1.getAccession());
            }

        } catch (URISyntaxException | IllegalAccessException e) {
            e.printStackTrace();
        }

    }


    public static RestTemplateCustomizer restTemplateCustomizer(BioSamplesProperties bioSamplesProperties) {
        return new RestTemplateCustomizer() {
            public void customize(RestTemplate restTemplate) {

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
                            if (value != null && param.equalsIgnoreCase
                                    ("timeout")) {
                                return Long.parseLong(value) * 1000;
                            }
                        }
                        //default to 60s if no header
                        return 60 * 1000;
                    }
                };

                //set a number of connections to use at once for multiple threads
                PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
                poolingHttpClientConnectionManager.setMaxTotal(bioSamplesProperties.getBiosamplesClientConnectionCountMax());
                poolingHttpClientConnectionManager.setDefaultMaxPerRoute(bioSamplesProperties.getBiosamplesClientConnectionCountDefault());

                //set a local cache for cacheable responses
                CacheConfig cacheConfig = CacheConfig.custom()
                        .setMaxCacheEntries(1024)
                        .setMaxObjectSize(1024 * 1024) //max size of 1Mb
                        //number of entries x size of entries = 1Gb total cache size
                        .setSharedCache(false) //act like a browser cache not a middle-hop cache
                        .build();

                //set a timeout limit
                //TODO put this in application.properties
                int timeout = 60; //in seconds
                RequestConfig config = RequestConfig.custom()
                        .setConnectTimeout(timeout * 1000) //time to establish the connection with the remote host
                        .setConnectionRequestTimeout(timeout * 1000) //maximum time of inactivity between two data packets
                        .setSocketTimeout(timeout * 1000).build(); //time to wait for a connection from the connection manager/pool


                //make the actual client
                HttpClient httpClient = CachingHttpClientBuilder.create()
                        .setCacheConfig(cacheConfig)
                        .useSystemProperties()
                        .setConnectionManager(poolingHttpClientConnectionManager)
                        .setKeepAliveStrategy(keepAliveStrategy)
                        .setDefaultRequestConfig(config)
                        .build();

                //and wire it into the resttemplate
                restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
            }
        };
    }
}
