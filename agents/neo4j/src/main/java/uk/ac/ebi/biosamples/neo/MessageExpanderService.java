package uk.ac.ebi.biosamples.neo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.repo.NeoCurationLinkRepository;
import uk.ac.ebi.biosamples.neo.repo.NeoCurationRepository;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;
import uk.ac.ebi.biosamples.neo.service.modelconverter.CurationToNeoCurationConverter;
import uk.ac.ebi.biosamples.neo.service.modelconverter.SampleToNeoSampleConverter;
import uk.ac.ebi.biosamples.service.SampleReadService;

@Service
public class MessageExpanderService {

	private final SampleReadService sampleReadService;
	
	public MessageExpanderService(SampleReadService sampleReadService) {
		this.sampleReadService = sampleReadService;
	}
	
	@Transactional
	public MessageContent expandMessage(MessageContent messageContent) {
		//include all samples from the derived hierarchy
		
		List<Sample> relatedSamples = new ArrayList<>();
		if (messageContent.getRelated() != null) {
			relatedSamples.addAll(messageContent.getRelated());
		}
		Queue<Sample> queue = new LinkedList<Sample>();
		queue.add(messageContent.getSample());
		while (!queue.isEmpty()) {
			Sample sample = queue.poll();
			relatedSamples.add(sample);
			for (Relationship relationship : sample.getRelationships()) {
				if (relationship.getSource().equals(sample.getAccession())
						&& relationship.getType().toLowerCase().equals("derived from")) {
					//this relationship points to another sample that it is derived from
					Sample derivedFrom = sampleReadService.fetch(relationship.getTarget());
					//already checked this one, skip it
					if (relatedSamples.contains(derivedFrom)) {
						continue;
					}
					//add it to the queue to follow that relationship again later
					queue.add(derivedFrom);
					//process it
					relatedSamples.add(derivedFrom);
				}
			}
		}
		return MessageContent.build(messageContent.getSample(), messageContent.getCurationLink(), 
				relatedSamples, messageContent.delete);
	}
}
