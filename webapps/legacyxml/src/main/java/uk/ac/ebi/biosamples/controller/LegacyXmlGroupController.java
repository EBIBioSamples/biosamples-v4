package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.model.legacyxml.BioSample;
import uk.ac.ebi.biosamples.model.legacyxml.BioSampleGroup;
import uk.ac.ebi.biosamples.model.legacyxml.ResultQuery;
import uk.ac.ebi.biosamples.service.FilterBuilder;
import uk.ac.ebi.biosamples.service.SummaryInfoService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Controller
public class LegacyXmlGroupController {


	private Logger log = LoggerFactory.getLogger(getClass());

	//TODO remove this in favour of using biosamples-client
	@Value("${biosamples.redirect.context}")
	private URI biosamplesRedirectContext;

	private final BioSamplesClient client;
	private final SummaryInfoService summaryInfoService;

	public LegacyXmlGroupController(BioSamplesClient client,
									SummaryInfoService summaryInfoService) {
		this.client = client;
		this.summaryInfoService = summaryInfoService;
	}

	@GetMapping(value="/groups/{accession:SAMEG\\d+}", produces={MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
	public void redirectGroups(@PathVariable String accession, HttpServletResponse response) throws IOException {
		String redirectUrl = String.format("%s/samples/%s.xml", biosamplesRedirectContext, accession);
		response.sendRedirect(redirectUrl);
	}

	@GetMapping(value = {"/groups"}, produces={MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
	public @ResponseBody ResultQuery getGroups(
			@RequestParam(name="query", required=true) String query,
			@RequestParam(name="pagesize", defaultValue = "25") int pagesize,
			@RequestParam(name="page", defaultValue = "1") int page,
			@RequestParam(name="sort", defaultValue = "desc") String sort
	) {
		if (page < 1) {
			throw new IllegalArgumentException("Page parameter has to be 1-based");
		}
		
		
		PagedResources<Resource<Sample>> results = client.fetchPagedSampleResource(query, page - 1, pagesize);
		
		ResultQuery resultQuery = new ResultQuery();
		
        resultQuery.setSummaryInfo(summaryInfoService.fromPagedGroupResources(results));
        
        for (Resource<Sample> resource : results.getContent()) {
        	BioSampleGroup bioSampleGroup = new BioSampleGroup();
        	bioSampleGroup.setId(resource.getContent().getAccession());
        	resultQuery.getBioSampleGroup().add(bioSampleGroup);
        }
        
        return resultQuery;
	}
	
	@GetMapping(value = {"/groupsamples/{groupAccession:SAMEG\\d+}"},
			produces={MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
	public @ResponseBody ResultQuery getSamplesInGroup(
			@PathVariable String groupAccession,
			@RequestParam(name="query", required=true) String query,
			@RequestParam(name="pagesize", defaultValue = "25") int pagesize,
			@RequestParam(name="page", defaultValue = "1") int page,
			@RequestParam(name="sort", defaultValue = "desc") String sort
	) {

        Filter relationFilter = FilterBuilder.create().onInverseRelation("has member").withValue(groupAccession).build();
//        Sort.Direction sort = Sort.Direction.fromString(queryParams.getOrDefault("sort","desc"));

		PagedResources<Resource<Sample>> results =
				client.fetchPagedSampleResource(query,
						Collections.singleton(relationFilter),
						page - 1,
						pagesize);

		ResultQuery resultQuery = new ResultQuery();

		resultQuery.setSummaryInfo(summaryInfoService.fromPagedGroupResources(results));

		for (Resource<Sample> resource : results.getContent()) {
			BioSample biosample = new BioSample();
			biosample.setId(resource.getContent().getAccession());
			resultQuery.getBioSample().add(biosample);
		}

		return resultQuery;
	}

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
