package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.ac.ebi.biosamples.model.ResultQuery;
import uk.ac.ebi.biosamples.model.Sort;
import uk.ac.ebi.biosamples.service.SampleService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/xml")
public class RedirectController {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Value("${biosamples.submissionuri}")
	private URI biosampleSubmissionUri;

	private SampleService sampleService;

	public RedirectController(SampleService sampleService) {
		this.sampleService = sampleService;
	}

	@RequestMapping(value="/samples/{accession}")
	public void redirectSample(@PathVariable String accession, HttpServletResponse response) throws IOException {
		String redirectUrl = String.format("%s/samples/%s", biosampleSubmissionUri, accession);
		response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML.getType());
		response.sendRedirect(redirectUrl);
	}

	@RequestMapping(value="/groups/{accession:SAMEG\\d+}")
	public void  redirectGroups(@PathVariable String accession, HttpServletResponse response) throws IOException {
		String redirectUrl = String.format("%s/groups/%s", biosampleSubmissionUri, accession);
		response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML.getType());
		response.sendRedirect(redirectUrl);
	}

	@RequestMapping(value = {"/samples", "/groups"}, produces = {MediaType.TEXT_XML_VALUE})
	public @ResponseBody String getSamples(
			@RequestParam String query,
			@RequestParam(defaultValue = "25") int size,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "desc") String sort
	) {
		ResultQuery result = ResultQuery.fromPagedResource(sampleService.getSamples(query, page, size, Sort.forParam(sort)));
	    return result.renderDocument();
	}


	// FIXME No groups is provided with the new BioSamples v4, not sure how to handle this
	@RequestMapping(value = {"/groupsamples/{groupAccession:SAMEG\\d+}/query={values}"}, produces = {MediaType.TEXT_XML_VALUE})
	public @ResponseBody String getSamplesInGroup(
			@PathVariable String groupAccession,
            @PathVariable String values
	) {
        Map<String, String> queryParams = readGroupSamplesQuery(values);
//        String query = String.format("%s AND %s", groupAccession, queryParams.get("text"));
        String query = queryParams.get("text");
        int size  = Integer.parseInt(queryParams.getOrDefault("size", "25"));
        int page  = Integer.parseInt(queryParams.getOrDefault("page", "1"));
        Sort sort = Sort.forParam(queryParams.getOrDefault("sort","desc"));
		ResultQuery result = ResultQuery.fromPagedResource(sampleService.getSamples(query,page, size, sort));
		return result.renderDocument();
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
