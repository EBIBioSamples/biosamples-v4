package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.neo4j.model.CypherQuery;
import uk.ac.ebi.biosamples.neo4j.model.GraphSearchQuery;
import uk.ac.ebi.biosamples.neo4j.model.GraphSearchRequest;
import uk.ac.ebi.biosamples.neo4j.model.GraphSearchResponse;
import uk.ac.ebi.biosamples.service.*;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;
import uk.ac.ebi.biosamples.utils.LinkUtils;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/samples/graph/search")
public class GraphSearchController {
    private static final Logger LOG = LoggerFactory.getLogger(GraphSearchController.class);

    private final SamplePageService samplePageService;
    private final SampleService sampleService;
    private final FilterService filterService;
    private final BioSamplesAapService bioSamplesAapService;
    private final SampleManipulationService sampleManipulationService;
    private final BioSamplesProperties bioSamplesProperties;
    private final SampleResourceAssembler sampleResourceAssembler;
    private final GraphSearchService graphSearchService;


    public GraphSearchController(
            SamplePageService samplePageService, FilterService filterService,
            BioSamplesAapService bioSamplesAapService,
            SampleResourceAssembler sampleResourceAssembler,
            SampleManipulationService sampleManipulationService,
            SampleService sampleService,
            BioSamplesProperties bioSamplesProperties,
            GraphSearchService graphSearchService) {
        this.samplePageService = samplePageService;
        this.filterService = filterService;
        this.bioSamplesAapService = bioSamplesAapService;
        this.sampleResourceAssembler = sampleResourceAssembler;
        this.sampleManipulationService = sampleManipulationService;
        this.sampleService = sampleService;
        this.bioSamplesProperties = bioSamplesProperties;
        this.graphSearchService = graphSearchService;
    }

    @PostMapping(path = "/cypher", consumes = { MediaType.APPLICATION_JSON_VALUE })
    public CypherQuery executeCypher(@RequestBody CypherQuery cypherQuery) {
        CypherQuery cypherQueryResponse = new CypherQuery();
        cypherQueryResponse.setQuery(cypherQuery.getQuery());
        cypherQueryResponse.setResponse(graphSearchService.executeCypher(cypherQuery.getQuery()));

        return cypherQueryResponse;
    }

    @PostMapping(path = "", consumes = { MediaType.APPLICATION_JSON_VALUE })
    public List<Map<String, Object>> graphSearch1(@RequestBody GraphSearchRequest request) {
        return graphSearchService.fetchByRelationship(request);
    }

    @PostMapping(path = "/links", consumes = { MediaType.APPLICATION_JSON_VALUE })
    public List<Sample> graphSearch(@RequestBody GraphSearchQuery query) {
        return graphSearchService.graphSearch(query);
    }


    @CrossOrigin(methods = RequestMethod.GET)
    @GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resources<Resource<Sample>>> searchHal(
            @RequestParam(name = "text", required = false) String text,
            @RequestParam(name = "filter", required = false) String[] filter,
            @RequestParam(name = "page", required = false) final Integer page,
            @RequestParam(name = "size", required = false) final Integer size,
            @RequestParam(name = "sort", required = false) final String[] sort,
            @RequestParam(name = "curationrepo", required = false) final String curationRepo,
            PagedResourcesAssembler<Sample> pageAssembler) {


        //Need to decode the %20 and similar from the parameters
        //this is *not* needed for the html controller
        String decodedText = LinkUtils.decodeText(text);
        String[] decodedFilter = LinkUtils.decodeTexts(filter);

        int effectivePage;
        if (page == null) {
            effectivePage = 0;
        } else {
            effectivePage = page;
        }
        int effectiveSize;
        if (size == null) {
            effectiveSize = 20;
        } else {
            effectiveSize = size;
        }

        Collection<Filter> filters = filterService.getFiltersCollection(decodedFilter);
        Collection<String> domains = bioSamplesAapService.getDomains();


        return null;
    }

}
