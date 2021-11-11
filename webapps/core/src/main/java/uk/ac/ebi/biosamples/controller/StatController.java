package uk.ac.ebi.biosamples.controller;

import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.model.facet.Facet;
import uk.ac.ebi.biosamples.mongo.model.MongoAnalytics;
import uk.ac.ebi.biosamples.service.StatService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stat")
public class StatController {
  private final StatService statService;

  public StatController(StatService statService) {
    this.statService = statService;
  }

  @CrossOrigin
  @GetMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @RequestMapping("/facets")
  public List<Facet> getFacets(
      @RequestParam(name = "text", required = false) String text,
      @RequestParam(name = "filter", required = false) String[] filter) {

    return statService.getSingleFacet(filter);
  }

  @CrossOrigin
  @GetMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @RequestMapping("/pipelines")
  public List<MongoAnalytics> getPipelineOutput() {

    return statService.getPipelineStats();
  }

  @CrossOrigin
  @GetMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @RequestMapping("/sample/growth")
  public Map<String, Integer> getSampleGrowth() {

    return statService.getBioSamplesYearlyGrowth();
  }

}


