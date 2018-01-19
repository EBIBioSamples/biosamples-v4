package uk.ac.ebi.biosamples.legacy.xml.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.exception.SampleNotFoundException;
import uk.ac.ebi.biosamples.legacy.xml.service.LegacyQueryParser;
import uk.ac.ebi.biosamples.legacy.xml.service.SummaryInfoService;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.model.legacyxml.BioSample;
import uk.ac.ebi.biosamples.model.legacyxml.ResultQuery;
import uk.ac.ebi.biosamples.service.FilterBuilder;
import uk.ac.ebi.biosamples.service.SampleToXmlConverter;

@RestController
public class LegacyXmlSampleController {


	private Logger log = LoggerFactory.getLogger(getClass());

	private final BioSamplesClient client;
	private final SummaryInfoService summaryInfoService;
	private final LegacyQueryParser legacyQueryParser;
	private final SampleToXmlConverter sampleToXmlConverter;

	private Filter sampleAccessionFilter = FilterBuilder.create().onAccession("SAM(N|D|EA|E)[0-9]+").build();

	public LegacyXmlSampleController(BioSamplesClient client,
									 SummaryInfoService summaryInfoService,
									 LegacyQueryParser legacyQueryParser,
									 SampleToXmlConverter sampleToXmlConverter) {
		this.client = client;
		this.summaryInfoService = summaryInfoService;
		this.legacyQueryParser = legacyQueryParser;
		this.sampleToXmlConverter = sampleToXmlConverter;
	}

	@CrossOrigin
	@GetMapping(value="/samples/{accession:SAM(?:N|D|EA|E)[0-9]+}", produces={MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
	public Sample getSample(@PathVariable String accession) throws IOException {
		Optional<Resource<Sample>> sample = client.fetchSampleResource(accession);
		
		if (sample.isPresent()) {
			log.trace("Found sample "+accession+" as "+sample.get());
			return sample.get().getContent();
		} else {
			log.trace("Did not find sample "+accession);
			throw new SampleNotFoundException();
		}
	}

	@CrossOrigin
	@GetMapping(value = {"/samples"}, produces={MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
	public ResultQuery getSamples(
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

		if (legacyQueryParser.queryContainsDateRangeFilter(query)) {

			Optional<Filter> dateRangeFilters = legacyQueryParser.extractDateFilterFromQuery(query);
			dateRangeFilters.ifPresent(filterList::add);
			query = legacyQueryParser.cleanQueryFromDateFilters(query);

		}

		if (legacyQueryParser.queryContainsSampleFilter(query)) {
			Optional<Filter> accessionFilter = legacyQueryParser.extractAccessionFilterFromQuery(query);
			accessionFilter.ifPresent(filterList::add);
			query = legacyQueryParser.cleanQueryFromAccessionFilter(query);

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
