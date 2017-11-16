package uk.ac.ebi.biosamples.legacy.xml.controller;

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
import uk.ac.ebi.biosamples.legacy.xml.service.LegacyQueryParser;
import uk.ac.ebi.biosamples.legacy.xml.service.SummaryInfoService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class LegacyXmlGroupController {


	private Logger log = LoggerFactory.getLogger(getClass());


	//TODO remove this in favour of using biosamples-client
	@Value("${biosamples.redirect.context}")
	private URI biosamplesRedirectContext;

	private final BioSamplesClient client;
	private final SummaryInfoService summaryInfoService;
	private final LegacyQueryParser legacyQueryParser;

	private final Filter groupAccessionFilter = FilterBuilder.create().onAccession("SAMEG[0-9]+").build();

	public LegacyXmlGroupController(BioSamplesClient client,
									SummaryInfoService summaryInfoService, LegacyQueryParser legacyQueryParser) {
		this.client = client;
		this.summaryInfoService = summaryInfoService;
		this.legacyQueryParser = legacyQueryParser;
	}

	@GetMapping(value="/groups/{accession:SAMEG\\d+}", produces={MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
	public void redirectGroups(@PathVariable String accession, HttpServletResponse response) throws IOException {
		String redirectUrl = String.format("%s/samples/%s.xml", biosamplesRedirectContext, accession);
		response.sendRedirect(redirectUrl);
	}

	@GetMapping(value = {"/groups"}, produces={MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
	public @ResponseBody ResultQuery getGroups(
			@RequestParam(name="query", defaultValue="*") String query,
			@RequestParam(name="pagesize", defaultValue = "25") int pagesize,
			@RequestParam(name="page", defaultValue = "1") int page,
			@RequestParam(name="sort", defaultValue = "desc") String sort
	) {
		if (page < 1) {
			throw new IllegalArgumentException("Page parameter has to be 1-based");
		}
		
		List<Filter> filterList = new ArrayList<>();
		filterList.add(groupAccessionFilter);

		if (legacyQueryParser.queryContainsDateRangeFilter(query)) {

			Optional<Filter> dateRangeFilters = legacyQueryParser.extractDateFilterFromQuery(query);
			dateRangeFilters.ifPresent(filterList::add);

		}

		if (legacyQueryParser.queryContainsSampleFilter(query)) {
			Optional<Filter> accessionFilter = legacyQueryParser.extractAccessionFilterFromQuery(query);
			accessionFilter.ifPresent(filterList::add);

		}

		query = legacyQueryParser.cleanQueryFromKnownFilters(query);


		PagedResources<Resource<Sample>> results = client.fetchPagedSampleResource(
				query,
				filterList,
				page -1,
				pagesize);

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
			@RequestParam(name="query", defaultValue="*") String query,
			@RequestParam(name="pagesize", defaultValue = "25") int pagesize,
			@RequestParam(name="page", defaultValue = "1") int page,
			@RequestParam(name="sort", defaultValue = "desc") String sort
	) {

//        Sort.Direction sort = Sort.Direction.fromString(queryParams.getOrDefault("sort","desc"));
		List<Filter> filterList = new ArrayList<>();
		filterList.add(FilterBuilder.create().onInverseRelation("has member").withValue(groupAccession).build());

		if (legacyQueryParser.queryContainsDateRangeFilter(query)) {

			Optional<Filter> dateRangeFilters = legacyQueryParser.extractDateFilterFromQuery(query);
			dateRangeFilters.ifPresent(filterList::add);

		}

		if (legacyQueryParser.queryContainsSampleFilter(query)) {
			Optional<Filter> accessionFilter = legacyQueryParser.extractAccessionFilterFromQuery(query);
			accessionFilter.ifPresent(filterList::add);

		}

		query = legacyQueryParser.cleanQueryFromKnownFilters(query);

		PagedResources<Resource<Sample>> results =
				client.fetchPagedSampleResource(query,
						filterList,
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

}
