package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.model.CurationLink;

@Service
public class CurationPersistService {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private AmqpTemplate amqpTemplate;
	
	
	public CurationLink store(CurationLink curationLink) {
		amqpTemplate.convertAndSend(Messaging.exchangeForIndexing, "", MessageContent.build(curationLink, false));
		return curationLink;
	}
	
}
