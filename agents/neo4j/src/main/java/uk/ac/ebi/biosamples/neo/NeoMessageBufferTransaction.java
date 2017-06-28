package uk.ac.ebi.biosamples.neo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoCuration;
import uk.ac.ebi.biosamples.neo.model.NeoCurationLink;
import uk.ac.ebi.biosamples.neo.model.NeoRelationship;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.repo.NeoCurationLinkRepository;
import uk.ac.ebi.biosamples.neo.repo.NeoCurationRepository;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;
import uk.ac.ebi.biosamples.neo.service.modelconverter.CurationToNeoCurationConverter;
import uk.ac.ebi.biosamples.neo.service.modelconverter.SampleToNeoSampleConverter;

@Component
public class NeoMessageBufferTransaction {


	private final NeoSampleRepository neoSampleRepository;
	private final SampleToNeoSampleConverter sampleToNeoSampleConverter;

	private final NeoCurationRepository neoCurationRepository;
	private final NeoCurationLinkRepository neoCurationLinkRepository;
	private final CurationToNeoCurationConverter curationToNeoCurationConverter;
	
	public NeoMessageBufferTransaction(NeoSampleRepository neoSampleRepository,
			SampleToNeoSampleConverter sampleToNeoSampleConverter,
			NeoCurationRepository neoCurationRepository,
			NeoCurationLinkRepository neoCurationLinkRepository,
			CurationToNeoCurationConverter curationToNeoCurationConverter) {
		this.neoSampleRepository = neoSampleRepository;
		this.sampleToNeoSampleConverter = sampleToNeoSampleConverter;
		this.neoCurationRepository = neoCurationRepository;
		this.neoCurationLinkRepository = neoCurationLinkRepository;
		this.curationToNeoCurationConverter = curationToNeoCurationConverter;
		
	}
	
	@Transactional
	public void save(Collection<MessageContent> messageContents) {		
		for (MessageContent messageContent : messageContents) {
			if (messageContent.hasSample()) {
				Sample sample = messageContent.getSample();
				NeoSample neoSample = sampleToNeoSampleConverter.convert(sample);
				
				//because relationships can refer to existing samples, make sure we use the existing NeoSample objects				
				Set<NeoRelationship> newRelationships = new HashSet<>();
				for (NeoRelationship oldRelationship : neoSample.getRelationships()) {
					NeoSample owner = oldRelationship.getOwner();
					NeoSample target = oldRelationship.getTarget();
					if (!owner.getAccession().equals(neoSample.getAccession())) {
						owner = neoSampleRepository.findOneByAccession(owner.getAccession(), 0);
						//if we couldn't find an existing one, use this dummy
						if (owner == null) {
							owner = oldRelationship.getOwner();
						}
					}
					if (!target.getAccession().equals(neoSample.getAccession())) {
						target = neoSampleRepository.findOneByAccession(target.getAccession(), 0);
						//if we couldn't find an existing one, use this dummy		
						if (target == null) {
							target = oldRelationship.getTarget();
						}
					}
					newRelationships.add(NeoRelationship.build(owner, oldRelationship.getType(), target));
				}
				neoSample.getRelationships().clear();
				neoSample.getRelationships().addAll(newRelationships);				
				
				neoSample = neoSampleRepository.save(neoSample, 1);
			}
			if (messageContent.hasCurationLink()) {				
				//NeoCuration neoCuration = curationToNeoCurationConverter.convert(messageContent.getCurationLink().getCuration());
				//make sure the neoCuration is saved
				//neoCuration = neoCurationRepository.save(neoCuration);

				//because can refer to an existing sample, need to make sure we use existing objects
				
				CurationLink curationLink = messageContent.getCurationLink();
				Curation curation = curationLink.getCuration();
				NeoCuration neoCuration = curationToNeoCurationConverter.convert(curation);
				neoCuration = neoCurationRepository.findOneByHash(neoCuration.getHash(), 1);
				if (neoCuration == null) {
					//no existing one, so make one to save
					neoCuration = curationToNeoCurationConverter.convert(curation);
				}
				
				NeoSample owner = neoSampleRepository.findOneByAccession(curationLink.getSample(), 0);
				if (owner == null) {
					//no existing sample, throw error
					throw new RuntimeException("CurationLink refers to non-existing sample "+curationLink.getSample());
				}
				
				
				NeoCurationLink neoCurationLink = NeoCurationLink.build(neoCuration, owner, curationLink.getDomain());
				
				neoCurationLink = neoCurationLinkRepository.save(neoCurationLink);
			}
		}
	}
}
