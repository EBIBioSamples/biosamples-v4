package uk.ac.ebi.biosamples.controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.GroupNode;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.SampleNode;
import uk.ac.ebi.arrayexpress2.sampletab.renderer.SampleTabWriter;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.ApiKeyService;
import uk.ac.ebi.biosamples.service.FilterBuilder;

@RestController
@RequestMapping("/v2")
public class XmlV2Controller {

	private final ApiKeyService apiKeyService;
	private final BioSamplesClient bioSamplesClient;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public XmlV2Controller(ApiKeyService apiKeyService, BioSamplesClient bioSamplesClient) {
		this.apiKeyService = apiKeyService;
		this.bioSamplesClient = bioSamplesClient;
	}

	/* sample end points below */
	@PostMapping(value = "/source/{source}/sample", 
			produces = MediaType.TEXT_PLAIN_VALUE, 
			consumes = {MediaType.APPLICATION_XML_VALUE})
	public ResponseEntity<String> saveSourceSampleNew(@PathVariable String source,
			@RequestBody(required=false) Sample sample, 
			@RequestParam String apikey) 
					throws ParseException, IOException {
		return saveSourceSample(source, UUID.randomUUID().toString(), sample, apikey);
	}
	
	@PostMapping(value = "/source/{source}/sample/{sourceid}", 
			produces = MediaType.TEXT_PLAIN_VALUE, 
			consumes = {MediaType.APPLICATION_XML_VALUE})
	public @ResponseBody ResponseEntity<String> saveSourceSample(@PathVariable String source,
			@PathVariable String sourceid, 
			@RequestBody(required=false) Sample sample, 
			@RequestParam String apikey)
					throws ParseException, IOException {
		
		
		
		//reject if has accession
		if (sample != null && sample.getAccession() != null) {
			return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Cannot POST a sample with an existing accession, use PUT for updates.");
		}
		
		// ensure source is case insensitive
		source = source.toLowerCase();
		Optional<String> keyOwner  = apiKeyService.getUsernameForApiKey(apikey);
		if (!keyOwner.isPresent()) {
			return new ResponseEntity<String>("Invalid API key ("+apikey+")", HttpStatus.FORBIDDEN);
		}

		if (!apiKeyService.canKeyOwnerEditSource(keyOwner.get(), source)) {
			return new ResponseEntity<String>("That API key is not permitted for that source", HttpStatus.FORBIDDEN);
		}
		
		//if no sample was provided, create one as a dummy with far future release date
		if (sample == null) {
			sample = Sample.build(sourceid, null, null, ZonedDateTime.now(ZoneOffset.UTC).plusYears(1000).toInstant(), 
					ZonedDateTime.now(ZoneOffset.UTC).toInstant(), 
					new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
		} else {
			//check provided sample has same name as sourceid
			if (!sample.getName().equals(sourceid)) {
				return new ResponseEntity<String>("Sample name mismatch ("+sourceid+" vs "+sample.getName()+")", HttpStatus.BAD_REQUEST);
			}
		}
		
		//update the sample to have the appropriate domain
		Optional<String> domain = apiKeyService.getDomainForApiKey(apikey);
		if (!domain.isPresent()) {
			return new ResponseEntity<String>("Invalid API key ("+apikey+")", HttpStatus.FORBIDDEN);
		}
		
		//reject if same name has been submitted before		
		List<Filter> filterList = new ArrayList<>(2);
		filterList.add(FilterBuilder.create().onName(sourceid).build());
		filterList.add(FilterBuilder.create().onDomain(domain.get()).build());
		if (bioSamplesClient.fetchSampleResourceAll(null, filterList).iterator().hasNext()) {
			return new ResponseEntity<String>("POST must be a new submission, use PUT for updates", HttpStatus.BAD_REQUEST);			
		}		
		
		sample = Sample.build(sample.getName(), sample.getAccession(), domain.get(), 
					sample.getRelease(), sample.getUpdate(), sample.getAttributes(), sample.getRelationships(), sample.getExternalReferences());

		//now actually do the submission
		sample = bioSamplesClient.persistSample(sample);
		
		//return the new accession
		return ResponseEntity.ok(sample.getAccession());
	}

	@PutMapping(value = "/source/{source}/sample/{sourceid}", 
			produces = MediaType.TEXT_PLAIN_VALUE, 
			consumes = {MediaType.APPLICATION_XML_VALUE})
	public @ResponseBody ResponseEntity<String> saveUpdate(@PathVariable String source, 
			@PathVariable String sourceid, @RequestParam String apikey, 
			@RequestBody(required=false) Sample sample) throws ParseException, IOException {
		
		//reject if not using biosample id
		if (!sourceid.matches("SAM[NED]A?[0-9]+")) {
			return new ResponseEntity<String>("Only implemented for BioSample accessions", HttpStatus.FORBIDDEN);
		}

		//reject if has no accession
		if (sample != null && sample.getAccession() == null) {
			return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Cannot PUT a sample without an existing accession, use POST for new samples.");
		}
		
		//TODO try and find any samples that already exist with this sourceid and use their accession
		//TODO if no existing sample, reject 
		
		// ensure source is case insensitive
		source = source.toLowerCase();
		Optional<String> keyOwner  = apiKeyService.getUsernameForApiKey(apikey);
		if (!keyOwner.isPresent()) {
			return new ResponseEntity<String>("Invalid API key ("+apikey+")", HttpStatus.FORBIDDEN);
		}

		if (!apiKeyService.canKeyOwnerEditSource(keyOwner.get(), source)) {
			return new ResponseEntity<String>("That API key is not permitted for that source", HttpStatus.FORBIDDEN);
		}
		
		//if no sample was provided, create one as a dummy with far future release date
		if (sample == null) {
			sample = Sample.build(sourceid, null, null, ZonedDateTime.now(ZoneOffset.UTC).plusYears(1000).toInstant(), 
					ZonedDateTime.now(ZoneOffset.UTC).toInstant(), 
					new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
		}
		
		//update the sample to have the appropriate domain
		Optional<String> domain = apiKeyService.getDomainForApiKey(apikey);
		if (!domain.isPresent()) {
			return new ResponseEntity<String>("Invalid API key ("+apikey+")", HttpStatus.FORBIDDEN);
		}
		sample = Sample.build(sample.getName(), sample.getAccession(), domain.get(), 
					sample.getRelease(), sample.getUpdate(), sample.getAttributes(), sample.getRelationships(), sample.getExternalReferences());

		//now actually do the submission
		sample = bioSamplesClient.persistSample(sample);
		
		//return the new accession
		return ResponseEntity.ok(sample.getAccession());
	}
	
	
/*
	@GetMapping(value = "/source/{source}/sample/{sourceid}", 
			produces = MediaType.TEXT_PLAIN_VALUE)
	public @ResponseBody ResponseEntity<String> getAccessionOfSample(@PathVariable String source,
			@PathVariable String sourceid, 
			@RequestParam String apikey) {
		
	}

	@GetMapping(value = "/source/{source}/sample/{sourceid}/submission", 
			produces = MediaType.TEXT_PLAIN_VALUE)
	public @ResponseBody ResponseEntity<String> getSubmissionOfSample(@PathVariable String source,
			@PathVariable String sourceid, 
			@RequestParam String apikey) {
		return null;
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
