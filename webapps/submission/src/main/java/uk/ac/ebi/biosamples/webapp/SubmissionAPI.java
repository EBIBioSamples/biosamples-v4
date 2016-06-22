package uk.ac.ebi.biosamples.webapp;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.models.MongoSample;
import uk.ac.ebi.biosamples.models.Sample;
import uk.ac.ebi.biosamples.models.SimpleSample;
import uk.ac.ebi.biosamples.repos.MongoSampleRepository;

@RestController
public class SubmissionAPI {

	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private MongoSampleRepository mongoSampleRepo;

	// @Autowired
	private RabbitMessagingTemplate rabbitTemplate;

	@Autowired
	public SubmissionAPI(RabbitMessagingTemplate rabbitTemplate, MessageConverter messageConverter) {
		this.rabbitTemplate = rabbitTemplate;
		this.rabbitTemplate.setMessageConverter(messageConverter);
		//this.rabbitTemplate.setDefaultDestination(Messaging.exchangeForLoading);
	}

	@RequestMapping(value = "/samples", method = RequestMethod.POST)
	public Sample newSample(@RequestBody Sample sample, HttpServletResponse response) throws AlreadySubmittedException, BadAccessionException {

		log.info("Recieved POST for "+sample.getAccession());
		
		if (sample.getAccession() == null || sample.getAccession().trim().length()==0) {
			throw new BadAccessionException();
		}
		
		//check if this has an accession
		//if this accession already exists
		Page<MongoSample> page = mongoSampleRepo.findByAccession(sample.getAccession(), new PageRequest(1,10, new Sort(Sort.Direction.DESC, "updateDate")));
		if (page.getTotalElements() > 0) {
			throw new AlreadySubmittedException();
		}
		
		//TODO set the update date to todays date?
		
		// create the new record
		MongoSample newSample = MongoSample.createFrom(sample);

		// persist the new sample
		newSample = mongoSampleRepo.save(newSample);

		// flag response that we created a sample
		response.setStatus(HttpServletResponse.SC_CREATED);

		// put in loading message queue
		rabbitTemplate.convertAndSend(Messaging.exchangeForLoading, "", SimpleSample.createFrom(newSample));

		return newSample;
	}

	@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Updates must have an accession")
	public class NotSubmittedException extends Exception {
	}

	@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Missing or invalid accession")
	public class BadAccessionException extends Exception {
	}

	@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "New samples cannot have an accession")
	public class AlreadySubmittedException extends Exception {
	}
}
