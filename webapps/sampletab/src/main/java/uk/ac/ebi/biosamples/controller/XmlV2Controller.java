package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.mongo.service.MongoAccessionService;
import uk.ac.ebi.biosamples.service.ApiKeyService;
import uk.ac.ebi.biosamples.service.FilterBuilder;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v2")
public class XmlV2Controller {

	private final ApiKeyService apiKeyService;
	private final BioSamplesClient bioSamplesClient;
	
	private final MongoAccessionService mongoGroupAccessionService;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public XmlV2Controller(ApiKeyService apiKeyService, BioSamplesClient bioSamplesClient, 
			@Qualifier("mongoGroupAccessionService") MongoAccessionService mongoGroupAccessionService) {
		this.apiKeyService = apiKeyService;
		this.bioSamplesClient = bioSamplesClient;
		this.mongoGroupAccessionService = mongoGroupAccessionService;
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
		
		//update the sample object with the domain
		//TODO support contact/publication/organization
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
			@RequestBody Sample sample) throws ParseException, IOException {
		
		//reject if not using biosample id
		if (!sourceid.matches("SAM[NED]A?[0-9]+")) {
			return new ResponseEntity<String>("Only implemented for BioSample accessions", HttpStatus.FORBIDDEN);
		}

		//reject if has no accession
		if (sample != null && sample.getAccession() == null) {
			return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Cannot PUT a sample without an existing accession, use POST for new samples.");
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
				
		//update the sample to have the appropriate domain
		Optional<String> domain = apiKeyService.getDomainForApiKey(apikey);
		if (!domain.isPresent()) {
			return new ResponseEntity<String>("Invalid API key ("+apikey+")", HttpStatus.FORBIDDEN);
		}
		
		//if no accession, try and find any samples that already exist with this sourceid and use their accession
		//NB we don't validate that a sample *must* have this sourceid
		if (sample.getAccession() == null) {
			//if no existing sample, reject 	
			List<Filter> filterList = new ArrayList<>(2);
			filterList.add(FilterBuilder.create().onName(sourceid).build());
			filterList.add(FilterBuilder.create().onDomain(domain.get()).build());
			Iterator<Resource<Sample>> it = bioSamplesClient.fetchSampleResourceAll(null, filterList).iterator();
			if (!it.hasNext()) {
				return new ResponseEntity<String>("PUT must be an update, use POST for new submissions", HttpStatus.BAD_REQUEST);			
			} else {
				Resource<Sample> first = it.next();
				if (it.hasNext()) {
					//error multiple accessions
					return new ResponseEntity<String>("Multiple samples with name "+sourceid+" in domain "+domain.get(), HttpStatus.BAD_REQUEST);	
				} else {
					sample = Sample.build(sample.getName(), first.getContent().getAccession(), null,
							 first.getContent().getRelease(),  first.getContent().getUpdate(), 
							 first.getContent().getAttributes(), first.getContent().getRelationships(), first.getContent().getExternalReferences());
				}
			}
		}

		//update the sample object with the domain
		sample = Sample.build(sample.getName(), sample.getAccession(), domain.get(), 
					sample.getRelease(), sample.getUpdate(), sample.getAttributes(), sample.getRelationships(), sample.getExternalReferences());

		//now actually do the submission
		sample = bioSamplesClient.persistSample(sample);
		
		//return the new accession
		return ResponseEntity.ok(sample.getAccession());
	}
	
	@GetMapping(value = "/source/{source}/sample/{sourceid}", 
			produces = MediaType.TEXT_PLAIN_VALUE)
	public @ResponseBody ResponseEntity<String> getAccessionOfSample(@PathVariable String source,
			@PathVariable String sourceid, 
			@RequestParam String apikey) {
		
		Optional<String> domain = apiKeyService.getDomainForApiKey(apikey);
		if (!domain.isPresent()) {
			return new ResponseEntity<String>("Invalid API key ("+apikey+")", HttpStatus.FORBIDDEN);
		}

		List<Filter> filterList = new ArrayList<>(2);
		filterList.add(FilterBuilder.create().onName(sourceid).build());
		filterList.add(FilterBuilder.create().onDomain(domain.get()).build());
		
		Iterator<Resource<Sample>> it = bioSamplesClient.fetchSampleResourceAll(null, filterList).iterator();
		
		Resource<Sample> first = null;
		if (it.hasNext()) {
			first = it.next();
			if (it.hasNext()) {
				return new ResponseEntity<String>("Multiple samples with sourceid "+sourceid, HttpStatus.BAD_REQUEST);
			} else {
				return new ResponseEntity<String>(first.getContent().getAccession(), HttpStatus.OK);
			}
		} else {
			return new ResponseEntity<String>(sourceid+" not recognized", HttpStatus.NOT_FOUND);
		}
	}
	
/*
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
	

	@PostMapping(value = "/source/{source}/group", produces = "text/plain", consumes = "application/xml")
	public ResponseEntity<String> saveSourceGroupNew(@PathVariable String source, 
			@RequestParam String apikey,
			@RequestBody(required=false) Sample group) 
					throws ParseException, IOException {
		return saveSourceGroup(source, UUID.randomUUID().toString(), apikey, group);
	}


	@PostMapping(value = "/source/{source}/group/{sourceid}", produces = "text/plain", consumes = "application/xml")
	public @ResponseBody ResponseEntity<String> saveSourceGroup(@PathVariable String source,
			@PathVariable String sourceid, 
			@RequestParam String apikey, 
			@RequestBody(required=false) Sample group)
					throws ParseException, IOException {
		

		//reject if has accession
		if (group != null && group.getAccession() != null) {
			return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Cannot POST a group with an existing accession, use PUT for updates.");
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
		if (group == null) {
			group = Sample.build(sourceid, null, null, ZonedDateTime.now(ZoneOffset.UTC).plusYears(1000).toInstant(), 
					ZonedDateTime.now(ZoneOffset.UTC).toInstant(), 
					new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
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
		
		//update the sample object with the domain
		group = Sample.build(group.getName(), group.getAccession(), domain.get(), 
				group.getRelease(), group.getUpdate(), group.getAttributes(), group.getRelationships(), group.getExternalReferences());
		
		//if accession is null, assign a new group accession		
		if (group.getAccession() == null) {
			group = mongoGroupAccessionService.generateAccession(group);
		}

		//now actually do the submission
		group = bioSamplesClient.persistSample(group);
		
		//return the new accession
		return ResponseEntity.ok(group.getAccession());
		
	}

	@PutMapping(value = "/source/{source}/group/{sourceid}", produces = "text/plain", consumes = "application/xml")
	public @ResponseBody ResponseEntity<String> saveGroupUpdate(@PathVariable String source, 
			@PathVariable String sourceid,
			@RequestParam String apikey, 
			@RequestBody Sample group) 
					throws ParseException, IOException {
		
		//reject if not using biosample id
		if (!sourceid.matches("SAMEG[0-9]+")) {
			return new ResponseEntity<String>("Only implemented for BioSample accessions", HttpStatus.FORBIDDEN);
		}

		//reject if has no accession
		if (group != null && group.getAccession() == null) {
			return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Cannot PUT a group without an existing accession, use POST for new samples.");
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
				
		//update the sample to have the appropriate domain
		Optional<String> domain = apiKeyService.getDomainForApiKey(apikey);
		if (!domain.isPresent()) {
			return new ResponseEntity<String>("Invalid API key ("+apikey+")", HttpStatus.FORBIDDEN);
		}
		
		//if no accession, try and find any samples that already exist with this sourceid and use their accession
		//NB we don't validate that a sample *must* have this sourceid
		if (group.getAccession() == null) {
			//if no existing sample, reject 	
			List<Filter> filterList = new ArrayList<>(2);
			filterList.add(FilterBuilder.create().onName(sourceid).build());
			filterList.add(FilterBuilder.create().onDomain(domain.get()).build());
			if (!bioSamplesClient.fetchSampleResourceAll(null, filterList).iterator().hasNext()) {
				return new ResponseEntity<String>("PUT must be an update, use POST for new submissions", HttpStatus.BAD_REQUEST);			
			}
			//TODO check only 1 match
			//TODO use accession of match
		}

		//update the sample object with the domain
		group = Sample.build(group.getName(), group.getAccession(), domain.get(), 
				group.getRelease(), group.getUpdate(), group.getAttributes(), group.getRelationships(), group.getExternalReferences());

		//now actually do the submission
		group = bioSamplesClient.persistSample(group);
		
		//return the new accession
		return ResponseEntity.ok(group.getAccession());
	}
	

/*
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
