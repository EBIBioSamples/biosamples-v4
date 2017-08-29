package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationLinkRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationRepository;
import uk.ac.ebi.biosamples.mongo.service.CurationLinkToMongoCurationLinkConverter;
import uk.ac.ebi.biosamples.mongo.service.CurationToMongoCurationConverter;
import uk.ac.ebi.biosamples.mongo.service.MongoCurationLinkToCurationLinkConverter;
import uk.ac.ebi.biosamples.mongo.service.MongoCurationToCurationConverter;

@Service
public class CurationPersistService {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private AmqpTemplate amqpTemplate;
	
	@Autowired
	private MongoCurationLinkRepository mongoCurationLinkRepository;
	@Autowired
	private CurationLinkToMongoCurationLinkConverter curationLinkToMongoCurationLinkConverter;
	@Autowired
	private MongoCurationLinkToCurationLinkConverter mongoCurationLinkToCurationLinkConverter;
	
	@Autowired
	private MongoCurationRepository mongoCurationRepository;
	@Autowired
	private CurationToMongoCurationConverter curationToMongoCurationConverter;
	@Autowired
	private MongoCurationToCurationConverter mongoCurationToCurationConverter;
	
	public CurationLink store(CurationLink curationLink) {

		//TODO do this as a trigger on the curation link repo
		//if it already exists, no need to save
		if (mongoCurationRepository.findOne(curationLink.getCuration().getHash()) == null) {
			mongoCurationRepository.save(curationToMongoCurationConverter.convert(curationLink.getCuration()));
		}

		//if it already exists, no need to save
		if (mongoCurationLinkRepository.findOne(curationLink.getHash()) == null) {
			curationLink = mongoCurationLinkToCurationLinkConverter.convert(mongoCurationLinkRepository.save(curationLinkToMongoCurationLinkConverter.convert(curationLink)));
		}
		
		amqpTemplate.convertAndSend(Messaging.exchangeForIndexingSolr, "", MessageContent.build(curationLink, false));
		return curationLink;
	}
	
}
