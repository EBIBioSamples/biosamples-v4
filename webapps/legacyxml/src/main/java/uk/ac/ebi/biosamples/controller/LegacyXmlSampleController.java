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
import uk.ac.ebi.biosamples.model.legacyxml.ResultQuery;
import uk.ac.ebi.biosamples.service.FilterBuilder;
import uk.ac.ebi.biosamples.service.LegacyQueryParser;
import uk.ac.ebi.biosamples.service.SummaryInfoService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class LegacyXmlSampleController {


	private Logger log = LoggerFactory.getLogger(getClass());

	//TODO remove this in favour of using biosamples-client
	@Value("${biosamples.redirect.context}")
	private URI biosamplesRedirectContext;

	private final BioSamplesClient client;
	private final SummaryInfoService summaryInfoService;
	private final LegacyQueryParser legacyQueryParser;

	private Filter sampleAccessionFilter = FilterBuilder.create().onAccession("SAM(N|D|EA|E)[0-9]+").build();

	public LegacyXmlSampleController(BioSamplesClient client,
									 SummaryInfoService summaryInfoService,
									 LegacyQueryParser legacyQueryParser) {
		this.client = client;
		this.summaryInfoService = summaryInfoService;
		this.legacyQueryParser = legacyQueryParser;
	}

	@GetMapping(value="/samples/{accession:SAM(?:N|D|EA|E)[0-9]+}", produces={MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
	public void redirectSample(@PathVariable String accession, HttpServletResponse response) throws IOException {
		//this is a little hacky, but in order to make sure that XML is returned (and only XML) from the 
		//content negotiation on the "real" endpoint, we need to use springs extension-based negotiation backdoor
		//THIS IS NOT W3C standards in any way!
		String redirectUrl = String.format("%s/samples/%s.xml", biosamplesRedirectContext, accession);
		response.sendRedirect(redirectUrl);
	}

	@GetMapping(value = {"/samples"}, produces={MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
	public @ResponseBody ResultQuery getSamples(
			@RequestParam(name="query", defaultValue = "*") String query,
			@RequestParam(name="pagesize", defaultValue = "25") int pagesize,
			@RequestParam(name="page", defaultValue = "1") int page,
			@RequestParam(name="sort", defaultValue = "desc") String sort
	)  {
	    if (page < 1) {
	        throw new IllegalArgumentException("Page parameter has to be 1-based");
		}


		List<Filter> filterList = new ArrayList<>();
	    filterList.add(sampleAccessionFilter);

	    if (legacyQueryParser.checkQueryContainsDateFilters(query)) {

	    	Optional<Filter> dateRangeFilters = legacyQueryParser.getDateFiltersFromQuery(query);
	    	dateRangeFilters.ifPresent(filterList::add);

	    	query = legacyQueryParser.cleanQueryFromDateFilters(query);
		}

		PagedResources<Resource<Sample>> results = client.fetchPagedSampleResource(
				query,
				filterList,
				page - 1,
				pagesize
		);
		
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
