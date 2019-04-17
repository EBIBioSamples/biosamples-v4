package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.CurationRule;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.CurationRulesService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/curations/rules")
public class CurationRulesRestController {
    private static final Logger LOG = LoggerFactory.getLogger(CurationRulesRestController.class);

    private final CurationRulesService curationRulesService;
    private final SampleResourceAssembler sampleResourceAssembler;

    public CurationRulesRestController(CurationRulesService curationRulesService,
                                       SampleResourceAssembler sampleResourceAssembler) {
        this.curationRulesService = curationRulesService;
        this.sampleResourceAssembler = sampleResourceAssembler;
    }

    @GetMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, String>> getAllCurationRules() {
        return ResponseEntity.ok().body(curationRulesService.getCurationRules());
    }

//    @RequestMapping("/save")
    @PostMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity saveCurationRule(@RequestBody CurationRule curationRule) {
        LOG.info("POST request, save curation rule: {}", curationRule);
        curationRulesService.saveCurationRule(curationRule.getAttributePre(), curationRule.getAttributePost());
        return ResponseEntity.ok().build();
    }

    @RequestMapping("/sample")
    @PostMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource<Sample>> getCuratedSample(@RequestBody Sample sample) {
        return ResponseEntity.ok().body(sampleResourceAssembler.toResource(curationRulesService.getCuratedSample(sample)));
    }

    @RequestMapping("/sample/curations")
    @PostMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<CurationLink>> getSuggestedCurations(@RequestBody Sample sample) {
        return ResponseEntity.ok().body(curationRulesService.getRuleBasedCurations(sample));
    }

    @RequestMapping("/sample/curations/save")
    @PostMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource<Sample>> saveRuleBasedCurations(@RequestBody Sample sample) {
        LOG.info("POST request, save rule based curations to sample: {}", sample);
        return ResponseEntity.ok().body(sampleResourceAssembler.toResource(curationRulesService.saveRuleBasedCurations(sample)));
    }
}

//class CurationRule {
//    private String attributePre;
//    private String attributePost;
//
//    public String getAttributePre() {
//        return attributePre;
//    }
//
//    public String getAttributePost() {
//        return attributePost;
//    }
//}
