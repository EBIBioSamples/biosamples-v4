package uk.ac.ebi.biosamples.neo;

import java.util.Collection;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoCuration;
import uk.ac.ebi.biosamples.neo.model.NeoCurationLink;
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
				neoSample = neoSampleRepository.save(neoSample);
			}
			if (messageContent.hasCurationLink()) {				
				NeoCuration neoCuration = curationToNeoCurationConverter.convert(messageContent.getCurationLink().getCuration());
				//make sure the neoCuration is saved
				neoCuration = neoCurationRepository.save(neoCuration);
				
				NeoSample neoSample = neoSampleRepository.findOneByAccession(messageContent.getCurationLink().getSample(),1);							
				NeoCurationLink neoCurationLink = NeoCurationLink.build(neoCuration, neoSample);
				neoCurationLink = neoCurationLinkRepository.save(neoCurationLink);
			}
		}
	}
}
