package uk.ac.ebi.biosamples.client.service;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.client.Traverson;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import uk.ac.ebi.biosamples.client.utils.IterableResourceFetchAll;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.StaticViewWrapper;
import uk.ac.ebi.biosamples.model.filter.Filter;

public class SampleCursorRetrievalService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	public static final DateTimeFormatter solrFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss'Z'");

	private static final ParameterizedTypeReference<PagedResources<Resource<Sample>>> parameterizedTypeReferencePagedResourcesSample = new ParameterizedTypeReference<PagedResources<Resource<Sample>>>(){};
	
	private final Traverson traverson;
	private final ExecutorService executor;
	private final RestOperations restOperations;
	private final int pageSize;

	
	
	public SampleCursorRetrievalService(RestOperations restOperations, Traverson traverson,
			ExecutorService executor, int pageSize) {
		this.restOperations = restOperations;
		this.traverson = traverson;
		this.executor = executor;
		this.pageSize = pageSize;
	}

	public Iterable<Resource<Sample>> fetchAll(String text, Collection<Filter> filterCollection) {
		return fetchAll(text, filterCollection, null);
	}

	public Iterable<Resource<Sample>> fetchAll(String text, Collection<Filter> filterCollection, String jwt) {
		return fetchAll(text, filterCollection, jwt, null);

	}

	public Iterable<Resource<Sample>> fetchAll(String text, Collection<Filter> filterCollection, String jwt, StaticViewWrapper.StaticView staticView) {

		MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
		params.add("text", text);
		for (Filter filter: filterCollection) {
			params.add("filter", filter.getSerialization());
		}
		params.add("size", Integer.toString(pageSize));
		if (staticView != null) {
			params.add("curationrepo", staticView.getCurationRepositoryName());
		}

		params = encodePlusInQueryParameters(params);

		return new IterableResourceFetchAll<Sample>(executor, traverson, restOperations,
				parameterizedTypeReferencePagedResourcesSample, jwt,
				params,	"samples", "cursor");

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
