package uk.ac.ebi.biosamples.controller;

import java.time.format.DateTimeFormatter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.model.SampleResource;
import uk.ac.ebi.biosamples.models.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoSubmission;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoSubmissionRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;
import uk.ac.ebi.biosamples.service.InverseRelationshipService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;

@RestController
@RequestMapping(value = "/samples")
@ExposesResourceFor(Sample.class)
public class SampleRestController {

	@Autowired
	private AmqpTemplate amqpTemplate;
	
	@Autowired
	private MongoSampleRepository mongoSampleRepository;
	@Autowired
	private MongoSubmissionRepository mongoSubmissionRepository;
	
	@Autowired
	private MongoSampleToSampleConverter mongoSampleToSampleConverter;
	@Autowired
	private SampleToMongoSampleConverter sampleToMongoSampleConverter;
	
	@Autowired
	private SampleResourceAssembler sampleResourceAssembler;
	
	@Autowired
	private InverseRelationshipService inverseRelationshipService;
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	@RequestMapping(method = RequestMethod.GET, value = "", produces = MediaTypes.HAL_JSON_VALUE)
	public ResponseEntity<PagedResources<SampleResource>> readAll(
            Pageable pageable,
            PagedResourcesAssembler<Sample> assembler) {
		Page<MongoSample> pageMongoSample = mongoSampleRepository.findAll(pageable);
		Page<Sample> pageSample = pageMongoSample.map(mongoSampleToSampleConverter);
		PagedResources<SampleResource> pagedResources = assembler.toResource(pageSample, sampleResourceAssembler);
		return ResponseEntity.ok(pagedResources);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "{accession}", produces = MediaTypes.HAL_JSON_VALUE)
	public ResponseEntity<SampleResource> read(@PathVariable String accession) {
		//return the raw sample from the repository
		MongoSample mongoSample = mongoSampleRepository.findOne(accession);		
		if (mongoSample == null) {
			//TODO return a 404 error
			throw new RuntimeException("Unable to find sample ("+accession+")");
		}
		
		//add any additional inverse relationships
		inverseRelationshipService.addInverseRelationships(mongoSample);
		
		//convert it into the format to return
		Sample sample = mongoSampleToSampleConverter.convert(mongoSample);
		SampleResource sampleResource = sampleResourceAssembler.toResource(sample);

		//create some http headers to populate for return
		HttpHeaders headers = new HttpHeaders();

		//add a last modified header
		//TODO add cors header?
		String lastModified = sampleResource.getUpdate().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss"))+" GMT";
		log.info("LastModified = "+lastModified);
		headers.set(HttpHeaders.LAST_MODIFIED, lastModified);
		 		
		//create the response object with the appropriate status
		ResponseEntity<SampleResource> response = new ResponseEntity<SampleResource>(sampleResource, headers, HttpStatus.OK);
		
		return response;
	}

	@RequestMapping(method = RequestMethod.PUT, value = "{accession}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public void update(@PathVariable String accession, @RequestBody Sample sample) {
		if (!sample.getAccession().equals(accession)) {
			//if the accession in the body is different to the accession in the url, throw an error
			//TODO create proper exception with right http error code
			throw new RuntimeException("Accessions must match ("+accession+" vs "+sample.getAccession()+")");
		}
		//TODO validate that relationships have this sample as the source
		
		//convert it to the storage specific version
		MongoSample mongoSample = sampleToMongoSampleConverter.convert(sample);
		//save the submission in the repository
		mongoSubmissionRepository.save(new MongoSubmission(mongoSample));
		//save the sample in the repository
		mongoSampleRepository.save(mongoSample);
		//send a message for further processing
		amqpTemplate.convertAndSend(Messaging.exchangeForIndexing, "", sample);
	}
}
