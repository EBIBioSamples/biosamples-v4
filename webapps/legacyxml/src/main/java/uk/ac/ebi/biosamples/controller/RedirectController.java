package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.ac.ebi.biosamples.model.BioSampleGroupResultQuery;
import uk.ac.ebi.biosamples.model.BioSampleResultQuery;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.Sort;
import uk.ac.ebi.biosamples.service.SampleService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class RedirectController {


	private Logger log = LoggerFactory.getLogger(getClass());

	//TODO remove this in favour of using biosamples-client
	@Value("${biosamples.redirect.context}")
	private URI biosamplesRedirectContext;

	private SampleService sampleService;

	public RedirectController(SampleService sampleService) {
		this.sampleService = sampleService;
	}

	@GetMapping(value="/samples/{accession}", produces={MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
	public void redirectSample(@PathVariable String accession, HttpServletResponse response) throws IOException {
		String redirectUrl = String.format("%s/samples/%s", biosamplesRedirectContext, accession);
		response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML.getType());
		response.sendRedirect(redirectUrl);
	}

	@GetMapping(value="/groups/{accession:SAMEG\\d+}", produces={MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
	public void  redirectGroups(@PathVariable String accession, HttpServletResponse response) throws IOException {
		String redirectUrl = String.format("%s/samples/%s", biosamplesRedirectContext, accession);
		response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML.getType());
		response.sendRedirect(redirectUrl);
	}

	@GetMapping(value = {"/samples"}, produces={MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
	public @ResponseBody
	BioSampleResultQuery getSamples(
			@RequestParam String query,
			@RequestParam(defaultValue = "25") int size,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "desc") String sort
	)  {
	    if (page < 1) {
	        throw new IllegalArgumentException("Page parameter has to be 1-based");
		}
		PagedResources<Resource<Sample>> results = sampleService.getPagedSamples(query, page - 1, size, Sort.forParam(sort));
        return BioSampleResultQuery.fromPagedResource(results);
	}

	@GetMapping(value = {"/groups"}, produces={MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
	public @ResponseBody
	BioSampleGroupResultQuery getGroups(
			@RequestParam String query,
			@RequestParam(defaultValue = "25") int size,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "desc") String sort
	) {
		if (page < 1) {
			throw new IllegalArgumentException("Page parameter has to be 1-based");
		}
		PagedResources<Resource<Sample>> results = sampleService.getPagedSamples(query, page - 1, size, Sort.forParam(sort));
        return BioSampleGroupResultQuery.fromPagedResource(results);
	}

//	// FIXME No groups is provided with the new BioSamples v4, not sure how to handle this
    // TODO Consider group relationships as attribute and solve this as search through attribute query
	@GetMapping(value = {"/groupsamples/{groupAccession:SAMEG\\d+}/query={values}"},  
			produces={MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
	public @ResponseBody BioSampleResultQuery getSamplesInGroup(
			@PathVariable String groupAccession,
            @PathVariable String values
	) {
		//TODO replace with a proper handling of arguments
        Map<String, String> queryParams = readGroupSamplesQuery(values);
//        String query = String.format("%s AND %s", groupAccession, queryParams.get("text"));
        String query = queryParams.get("text");
        int size  = Integer.parseInt(queryParams.getOrDefault("size", "25"));
        int page  = Integer.parseInt(queryParams.getOrDefault("page", "1"));
        Sort sort = Sort.forParam(queryParams.getOrDefault("sort","desc"));
		PagedResources<Resource<Sample>> results = sampleService.getPagedSamples(query, page - 1, size, sort);
		return BioSampleResultQuery.fromPagedResource(results);
	}

//	private String getTextQuery(String[] queries, String delimiter) {
//		return queries.length > 1 ? String.join(delimiter, queries) : Optional.of(queries[0]).orElse(null);
//	}

//	private String getTextQuery(String[] queries) {
//        return getTextQuery(queries, " OR ");
//	}

	private Map<String, String> readGroupSamplesQuery(String query) {
        Map<String, String> queryParams = new HashMap<>();
        String[] params = query.split("&");
        for(String param: params) {
            if(param.contains("=")) {
                String[] keyValue = param.split("=",1);
                queryParams.put(keyValue[0],keyValue[1]);
            } else {
                queryParams.put("text", param);
            }
        }
        return queryParams;
    }

}
