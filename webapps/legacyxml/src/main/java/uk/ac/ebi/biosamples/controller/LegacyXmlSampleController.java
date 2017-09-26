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
import uk.ac.ebi.biosamples.model.legacyxml.BioSample;
import uk.ac.ebi.biosamples.model.legacyxml.ResultQuery;
import uk.ac.ebi.biosamples.service.SummaryInfoService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

@Controller
public class LegacyXmlSampleController {


	private Logger log = LoggerFactory.getLogger(getClass());

	//TODO remove this in favour of using biosamples-client
	@Value("${biosamples.redirect.context}")
	private URI biosamplesRedirectContext;

	private final BioSamplesClient client;
	private final SummaryInfoService summaryInfoService;

	public LegacyXmlSampleController(BioSamplesClient client, SummaryInfoService summaryInfoService) {
		this.client = client;
		this.summaryInfoService = summaryInfoService;
	}

	@GetMapping(value="/samples/{accession}", produces={MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
	public void redirectSample(@PathVariable String accession, HttpServletResponse response) throws IOException {
		//this is a little hacky, but in order to make sure that XML is returned (and only XML) from the 
		//content negotiation on the "real" endpoint, we need to use springs extension-based negotiation backdoor
		//THIS IS NOT W3C standards in any way!
		String redirectUrl = String.format("%s/samples/%s.xml", biosamplesRedirectContext, accession);
		response.sendRedirect(redirectUrl);
	}

	@GetMapping(value = {"/samples"}, produces={MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
	public @ResponseBody ResultQuery getSamples(
			@RequestParam(name="query", required=true) String query,
			@RequestParam(name="pagesize", defaultValue = "25") int pagesize,
			@RequestParam(name="page", defaultValue = "1") int page,
			@RequestParam(name="sort", defaultValue = "desc") String sort
	)  {
	    if (page < 1) {
	        throw new IllegalArgumentException("Page parameter has to be 1-based");
		}
		PagedResources<Resource<Sample>> results = client.fetchPagedSamples(query, page - 1, pagesize);
		
		ResultQuery resultQuery = new ResultQuery();
		
        resultQuery.setSummaryInfo(summaryInfoService.fromPagedSampleResources(results));
        
        for (Resource<Sample> resource : results.getContent()) {
        	BioSample bioSample = new BioSample();
        	bioSample.setId(resource.getContent().getAccession());
        	resultQuery.getBioSample().add(bioSample);
        }
        
        return resultQuery;
	}

}
