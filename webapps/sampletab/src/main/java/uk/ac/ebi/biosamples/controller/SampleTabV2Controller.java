package uk.ac.ebi.biosamples.controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.GroupNode;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.SampleNode;
import uk.ac.ebi.arrayexpress2.sampletab.renderer.SampleTabWriter;

@Controller
@RequestMapping("/v2")
public class SampleTabV2Controller {

	private Logger log = LoggerFactory.getLogger(getClass());

	public SampleTabV2Controller() {

	}

	/* sample end points below */
/*	
	@PostMapping(value = "/source/{source}/sample", produces = "text/plain")
	public ResponseEntity<String> accessionSourceSampleNew(@PathVariable String source, 
			@RequestParam String apikey) {
		return accessionSourceSample(source, UUID.randomUUID().toString(), apikey);
	}

	@PostMapping(value = "/source/{source}/sample", produces = "text/plain", consumes = "application/xml")
	public ResponseEntity<String> saveSourceSampleNew(@PathVariable String source, 
			@RequestParam String apikey,
			@RequestBody BioSampleType sample) 
					throws ParseException, IOException {
		return saveSourceSample(source, UUID.randomUUID().toString(), apikey, sample);
	}

	@PostMapping(value = "/source/{source}/sample/{sourceid}", produces = "text/plain")
	public @ResponseBody ResponseEntity<String> accessionSourceSample(@PathVariable String source,
			@PathVariable String sourceid, 
			@RequestParam String apikey) {
		
	}

	@PostMapping(value = "/source/{source}/sample/{sourceid}", produces = "text/plain", consumes = "application/xml")
	public @ResponseBody ResponseEntity<String> saveSourceSample(@PathVariable String source,
			@PathVariable String sourceid, 
			@RequestParam String apikey, 
			@RequestBody BioSampleType sample)
					throws ParseException, IOException {
	}

	@PutMapping(value = "/source/{source}/sample/{sourceid}", produces = "text/plain", consumes = "application/xml")
	public @ResponseBody ResponseEntity<String> saveUpdate(@PathVariable String source, 
			@PathVariable String sourceid,
			@RequestParam String apikey, 
			@RequestBody BioSampleType sample) throws ParseException, IOException {
	}
	
	
	@GetMapping(value = "/source/{source}/sample/{sourceid}", produces = "text/plain")
	public @ResponseBody ResponseEntity<String> getAccessionOfSample(@PathVariable String source,
			@PathVariable String sourceid, 
			@RequestParam String apikey) {
		
	}

	@GetMapping(value = "/source/{source}/sample/{sourceid}/submission", produces = "text/plain")
	public @ResponseBody ResponseEntity<String> getSubmissionOfSample(@PathVariable String source,
			@PathVariable String sourceid, 
			@RequestParam String apikey) {
		
	}	
*/	
	/* sample end points above */
	/* group end points below */
/*	
	@PostMapping(value = "/source/{source}/group", produces = "text/plain")
	public ResponseEntity<String> accessionSourceGroupNew(@PathVariable String source, 
			@RequestParam String apikey) {
		return accessionSourceGroup(source, UUID.randomUUID().toString(), apikey);
	}

	@PostMapping(value = "/source/{source}/group", produces = "text/plain", consumes = "application/xml")
	public ResponseEntity<String> saveSourceGroupNew(@PathVariable String source, 
			@RequestParam String apikey,
			@RequestBody BioSampleGroupType group) 
					throws ParseException, IOException {
		return saveSourceGroup(source, UUID.randomUUID().toString(), apikey, group);
	}

	@PostMapping(value = "/source/{source}/group/{sourceid}", produces = "text/plain")
	public @ResponseBody ResponseEntity<String> accessionSourceGroup(@PathVariable String source,
			@PathVariable String sourceid, 
			@RequestParam String apikey) {
		
	}

	@PostMapping(value = "/source/{source}/group/{sourceid}", produces = "text/plain", consumes = "application/xml")
	public @ResponseBody ResponseEntity<String> saveSourceGroup(@PathVariable String source,
			@PathVariable String sourceid, 
			@RequestParam String apikey, 
			@RequestBody BioSampleGroupType group)
					throws ParseException, IOException {
	}

	@PutMapping(value = "/source/{source}/group/{sourceid}", produces = "text/plain", consumes = "application/xml")
	public @ResponseBody ResponseEntity<String> saveGroupUpdate(@PathVariable String source, 
			@PathVariable String sourceid,
			@RequestParam String apikey, 
			@RequestBody BioSampleGroupType group) 
					throws ParseException, IOException {
	}
	
	
	@GetMapping(value = "/source/{source}/group/{sourceid}", produces = "text/plain")
	public @ResponseBody ResponseEntity<String> getAccessionOfGroup(@PathVariable String source,
			@PathVariable String sourceid, 
			@RequestParam String apikey) {
	}

	@GetMapping(value = "/source/{source}/group/{sourceid}/submission", produces = "text/plain")
	public @ResponseBody ResponseEntity<String> getSubmissionOfGroup(@PathVariable String source,
			@PathVariable String sourceid, 
			@RequestParam String apikey) {
	}	
*/
	/* group endpoints above*/
	
}
