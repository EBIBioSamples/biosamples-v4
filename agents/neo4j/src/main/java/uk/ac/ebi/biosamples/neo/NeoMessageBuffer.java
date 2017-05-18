package uk.ac.ebi.biosamples.neo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.messages.threaded.MessageBuffer;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.ExternalReferenceLink;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoCurationLink;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReferenceLink;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.repo.NeoCurationLinkRepository;
import uk.ac.ebi.biosamples.neo.repo.NeoExternalReferenceLinkRepository;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;
import uk.ac.ebi.biosamples.neo.service.modelconverter.CurationLinkToNeoCurationLinkConverter;
import uk.ac.ebi.biosamples.neo.service.modelconverter.ExternalReferenceLinkToNeoExternalReferenceLinkConverter;
import uk.ac.ebi.biosamples.neo.service.modelconverter.SampleToNeoSampleConverter;

@Component
public class NeoMessageBuffer extends MessageBuffer<MessageContent> {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final NeoSampleRepository neoSampleRepository;
	private final SampleToNeoSampleConverter sampleToNeoSampleConverter;
	
	private final NeoExternalReferenceLinkRepository neoExternalReferenceLinkRepository;
	private final ExternalReferenceLinkToNeoExternalReferenceLinkConverter externalReferenceLinkToNeoExternalReferenceLinkConverter;
	
	private final NeoCurationLinkRepository neoCurationLinkRepository;
	private final CurationLinkToNeoCurationLinkConverter curationLinkToNeoCurationLinkConverter;
	
	public NeoMessageBuffer(AgentNeo4JProperties properties, NeoSampleRepository neoSampleRepository,
			SampleToNeoSampleConverter sampleToNeoSampleConverter,
			ExternalReferenceLinkToNeoExternalReferenceLinkConverter externalReferenceLinkToNeoExternalReferenceLinkConverter,
			NeoExternalReferenceLinkRepository neoExternalReferenceLinkRepository,
			CurationLinkToNeoCurationLinkConverter curationLinkToNeoCurationLinkConverter,
			NeoCurationLinkRepository neoCurationLinkRepository) {
		super(properties.getAgentNeo4JQueueSize(), properties.getAgentNeo4JQueueTime());
		this.neoSampleRepository = neoSampleRepository;
		this.sampleToNeoSampleConverter = sampleToNeoSampleConverter;
		this.neoExternalReferenceLinkRepository = neoExternalReferenceLinkRepository;
		this.externalReferenceLinkToNeoExternalReferenceLinkConverter = externalReferenceLinkToNeoExternalReferenceLinkConverter;
		this.neoCurationLinkRepository = neoCurationLinkRepository;
		this.curationLinkToNeoCurationLinkConverter = curationLinkToNeoCurationLinkConverter;
	}

	@Override
	protected void save(Collection<MessageContent> messageContents) {
		
		Collection<NeoSample> samples = new ArrayList<>();
		Collection<NeoExternalReferenceLink> externalRefenceLinks = new ArrayList<>();
		Collection<NeoCurationLink> curationLinks = new ArrayList<>();
		
		for (MessageContent messageContent : messageContents) {
			if (messageContent.hasSample()) {
				Sample sample = messageContent.getSample();
				NeoSample neoSample = sampleToNeoSampleConverter.convert(sample);
				samples.add(neoSample);
			}
			if (messageContent.hasExternalReferenceLink()) {
				ExternalReferenceLink externalRefrerenceLink = messageContent.getExternalReferenceLink();
				NeoExternalReferenceLink neoExternalReferenceLink = externalReferenceLinkToNeoExternalReferenceLinkConverter.convert(externalRefrerenceLink);
				externalRefenceLinks.add(neoExternalReferenceLink);
			}
			if (messageContent.hasCurationLink()) {
				CurationLink curationLink = messageContent.getCurationLink();
				NeoCurationLink neoCurationLink = curationLinkToNeoCurationLinkConverter.convert(curationLink);
				curationLinks.add(neoCurationLink);
			}
		}
		//TODO make this a single transaction instead of 3 transactions
		neoSampleRepository.save(samples);
		neoExternalReferenceLinkRepository.save(externalRefenceLinks);
		neoCurationLinkRepository.save(curationLinks);
	}

}
