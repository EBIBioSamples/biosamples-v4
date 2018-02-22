package uk.ac.ebi.biosamples.client.service;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.client.utils.IterableResourceFetchAll;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;

public class SamplePageRetrievalService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	public static final DateTimeFormatter solrFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss'Z'");

	private static final ParameterizedTypeReference<PagedResources<Resource<Sample>>> parameterizedTypeReferencePagedResourcesSample = new ParameterizedTypeReference<PagedResources<Resource<Sample>>>(){};
	
	private final Traverson traverson;
	private final ExecutorService executor;
	private final RestOperations restOperations;
	private final int pageSize;

	
	
	public SamplePageRetrievalService(RestOperations restOperations, Traverson traverson,
			ExecutorService executor, int pageSize) {
		this.restOperations = restOperations;
		this.traverson = traverson;
		this.executor = executor;
		this.pageSize = pageSize;
	}

	
	public PagedResources<Resource<Sample>> search(String text, Collection<Filter> filters, int page, int size) {
		MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
		params.add("page", Integer.toString(page));
		params.add("size", Integer.toString(size));
		params.add("searchTerm", !text.isEmpty() ? text : "*:*");
		for (Filter filter: filters) {
            params.add("filter", filter.getSerialization());
		}

		params = encodePlusInQueryParameters(params);

		URI uri = UriComponentsBuilder.fromUriString(traverson.follow("samples").asLink().getHref())
				.queryParams(params)
				.build()
				.toUri();

		log.trace("GETing " + uri);

		RequestEntity<Void> requestEntity = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
		ResponseEntity<PagedResources<Resource<Sample>>> responseEntity = restOperations.exchange(requestEntity,
				new ParameterizedTypeReference<PagedResources<Resource<Sample>>>() {
				});

		if (!responseEntity.getStatusCode().is2xxSuccessful()) {
			throw new RuntimeException("Problem GETing samples");
		}


		log.trace("GETted " + uri);

		return responseEntity.getBody();
	}

	public Iterable<Resource<Sample>> fetchAll(String text, Collection<Filter> filterCollection) {
		MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
		params.add("text", text);
		for (Filter filter: filterCollection) {
			params.add("filter", filter.getSerialization());
		}
		params.add("size", Integer.toString(pageSize));

		params = encodePlusInQueryParameters(params);

		return new IterableResourceFetchAll<Sample>(executor, traverson, restOperations,
				parameterizedTypeReferencePagedResourcesSample,
				params,	"samples");

	}

    // TODO to keep the + in a (not encoded) query parameter is to force encoding
	private MultiValueMap<String, String> encodePlusInQueryParameters(MultiValueMap<String, String> queryParameters) {
	    MultiValueMap<String,String> encodedQueryParameters = new LinkedMultiValueMap<>();
	    for (Map.Entry<String, List<String>> param: queryParameters.entrySet()) {
	        	String key = param.getKey();
	        	param.getValue().forEach(v -> {
					if (v != null) {
						encodedQueryParameters.add(key, v.replaceAll("\\+", "%2B"));
					} else {
						encodedQueryParameters.add(key, "");
					}
				});
        }
        return encodedQueryParameters;
    }

}
