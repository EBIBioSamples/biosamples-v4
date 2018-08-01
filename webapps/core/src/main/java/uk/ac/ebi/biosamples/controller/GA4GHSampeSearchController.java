package uk.ac.ebi.biosamples.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.model.ga4gh.Ga4ghSample;
import uk.ac.ebi.biosamples.service.GA4GHFilterBuilder;
import uk.ac.ebi.biosamples.service.Ga4ghSampleResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleToGa4ghSampleConverter;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "samples/ga4gh", produces = "application/json")
public class GA4GHSampeSearchController {

    private GA4GHFilterBuilder filterBuilder;
    private SamplesRestController restController;
    private SampleToGa4ghSampleConverter mapper;
    private Ga4ghSampleResourceAssembler resourceAssembler;

    @Autowired
    public GA4GHSampeSearchController(GA4GHFilterBuilder filterBuilder, SamplesRestController controller, SampleToGa4ghSampleConverter mapper, Ga4ghSampleResourceAssembler resourceAssembler) {
        this.filterBuilder = filterBuilder;
        this.restController = controller;
        this.mapper = mapper;
        this.resourceAssembler = resourceAssembler;
    }

    @RequestMapping(method = RequestMethod.GET, produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public ResponseEntity<Resources<Resource<Ga4ghSample>>> searchSample(@RequestParam(name = "disease") String disease, @RequestParam(name = "page") int page) {

        Collection<Filter> filters = filterBuilder.getFilters();
        List<String> filtersAsText = filters.parallelStream()
                .map(
                        Filter::getSerialization
                )
                .collect(Collectors.toList());
        String[] filtersAsTextArray = new String[1];
        filtersAsTextArray = filtersAsText.toArray(filtersAsTextArray);
        ResponseEntity<Resources<Resource<Sample>>> response = restController.searchHal(disease, filtersAsTextArray, null, page, null, null, null);
        Resources<Resource<Sample>> samples = response.getBody();
        List<Resource<Ga4ghSample>> ga4ghSamples = samples.getContent().stream()
                .map(
                        i -> {
                            Sample sample = i.getContent();
                            Ga4ghSample ga4ghSample = mapper.convert(sample);
                            return resourceAssembler.toResource(ga4ghSample);
                        }
                )
                .collect(Collectors.toList());
        Resources<Resource<Ga4ghSample>> resources = new Resources<>(ga4ghSamples);
        return new ResponseEntity<>(resources, HttpStatus.OK);

    }

}
