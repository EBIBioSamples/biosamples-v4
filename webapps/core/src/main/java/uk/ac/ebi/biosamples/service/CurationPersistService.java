package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.mongo.model.MongoCuration;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationLinkRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationRepository;
import uk.ac.ebi.biosamples.mongo.service.CurationLinkToMongoCurationLinkConverter;
import uk.ac.ebi.biosamples.mongo.service.CurationToMongoCurationConverter;
import uk.ac.ebi.biosamples.mongo.service.MongoCurationLinkToCurationLinkConverter;

@Service
public class CurationPersistService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	//TODO use constructor injection
	
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
	private MessagingService messagingSerivce;
	
	public CurationLink store(CurationLink curationLink) {

		//TODO do this as a trigger on the curation link repo
		//if it already exists, no need to save
		if (mongoCurationRepository.findOne(curationLink.getCuration().getHash()) == null) {
			MongoCuration mongoCuration = curationToMongoCurationConverter.convert(curationLink.getCuration());
			try {
				mongoCurationRepository.save(mongoCuration);
			} catch (DuplicateKeyException e) {
				//sometimes, if there are multiple threads there may be a collision
				//check if its a true duplicate and not an accidental hash collision
				MongoCuration existingMongoCuration = mongoCurationRepository.findOne(mongoCuration.getHash());
				if (!existingMongoCuration.equals(mongoCuration)) {
					//if it is a different curation with an hash collision, then throw an exception
					throw e;
				}
				
			}
		}

		//if it already exists, no need to save
		if (mongoCurationLinkRepository.findOne(curationLink.getHash()) == null) {
			curationLink = mongoCurationLinkToCurationLinkConverter.convert(mongoCurationLinkRepository.save(curationLinkToMongoCurationLinkConverter.convert(curationLink)));
		}

		messagingSerivce.sendMessages(curationLink);
		return curationLink;
	}
	
}
