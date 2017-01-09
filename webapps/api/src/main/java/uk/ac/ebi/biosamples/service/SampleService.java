package uk.ac.ebi.biosamples.service;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.models.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoSubmission;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoSubmissionRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;


/**
 * Service layer buisness logic layer for centralizing repository access and conversions between different controllers
 * 
 * @author faulcon
 *
 */
@Service
public class SampleService {

	@Autowired
	private MongoSampleRepository mongoSampleRepository;
	@Autowired
	private MongoSubmissionRepository mongoSubmissionRepository;

	@Autowired
	private SolrSampleRepository solrSampleRepository;
	
	@Autowired
	private InverseRelationshipService inverseRelationshipService;

	@Autowired
	private AmqpTemplate amqpTemplate;
	
	@Autowired
	private MongoSampleToSampleConverter mongoSampleToSampleConverter;
	@Autowired
	private SampleToMongoSampleConverter sampleToMongoSampleConverter;
		
	public Sample fetch(String accession) {
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
		return sample;
	}
	
	public Page<Sample> fetchFindAll(Pageable pageable) {
		//return the raw sample from the repository
		Page<MongoSample> pageMongoSample = mongoSampleRepository.findAll(pageable);
		//add any additional inverse relationships
		for (MongoSample mongoSample : pageMongoSample) {
			inverseRelationshipService.addInverseRelationships(mongoSample);
		}
		//convert it into the format to return
		Page<Sample> pageSample = pageMongoSample.map(mongoSampleToSampleConverter);
		return pageSample;
	}
	
	public Page<Sample> fetchFindByText(String text, Pageable pageable) {
		//return the raw sample from the repository
		Page<SolrSample> pageSolrSample = solrSampleRepository.findByText(text, pageable);
		//fetch the version from Mongo and add inverse relationships
		Page<Sample> pageSample = pageSolrSample.map(s -> fetch(s.getAccession()));		
		return pageSample;
	}
	
	public void store(Sample sample) {

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
