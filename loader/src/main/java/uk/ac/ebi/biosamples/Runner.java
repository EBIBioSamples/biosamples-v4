package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.biosamples.models.JPASample;
import uk.ac.ebi.biosamples.models.Messaging;
import uk.ac.ebi.biosamples.models.SimpleSample;
import uk.ac.ebi.biosamples.repos.JPASampleRepository;

@Component
public class Runner implements ApplicationRunner {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	private RabbitMessagingTemplate rabbitTemplate;
	
	@Autowired
	private JPASampleRepository jpaSampleRepository;
	
	public Runner(RabbitMessagingTemplate rabbitTemplate, MessageConverter messageConverter) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setMessageConverter(messageConverter);		
	}
	
	@Transactional
    public void recieveForLoading(SimpleSample sample) {
    	log.info("Recieved "+sample.getAccession());
    	//convert it to the right object type
    	JPASample jpaSample = JPASample.createFrom(sample);
    	JPASample oldSample = jpaSampleRepository.findByAccession(jpaSample.getAccession());
    	if (oldSample != null) {
    		//remove any existing sample
    		log.info("Updating previous sample "+sample.getAccession());
    		jpaSampleRepository.delete(oldSample);
    	} 
		//save the new one
		log.info("Saving sample "+sample.getAccession());
		jpaSample = jpaSampleRepository.save(jpaSample);
		
		
		//NOTES:
		// old attributes are never removed? only reused?
    }

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Starting...");
		SimpleSample sample;
		boolean processing = true;
		while (processing) {
			sample = rabbitTemplate.receiveAndConvert(Messaging.queueToBeLoaded, SimpleSample.class);
			if (sample != null) {
				recieveForLoading(sample);
			} else {
				//no more messages
				if (args.containsOption("always")) {
					//sleep for a moment to allow more messages to arrive
					Thread.sleep(1000);
				} else {
					processing = false;
				}
			}
		}
		log.info("Exiting ...");
	}

}
