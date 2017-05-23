package uk.ac.ebi.biosamples.neo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.messages.threaded.MessageBuffer;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.ExternalReferenceLink;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoAttribute;
import uk.ac.ebi.biosamples.neo.model.NeoCuration;
import uk.ac.ebi.biosamples.neo.model.NeoCurationLink;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReferenceLink;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.repo.NeoCurationLinkRepository;
import uk.ac.ebi.biosamples.neo.repo.NeoCurationRepository;
import uk.ac.ebi.biosamples.neo.repo.NeoExternalReferenceLinkRepository;
import uk.ac.ebi.biosamples.neo.repo.NeoExternalReferenceRepository;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;
import uk.ac.ebi.biosamples.neo.service.modelconverter.SampleToNeoSampleConverter;

@Component
public class NeoMessageBuffer extends MessageBuffer<MessageContent> {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final NeoSampleRepository neoSampleRepository;
	private final SampleToNeoSampleConverter sampleToNeoSampleConverter;

	private final NeoExternalReferenceLinkRepository neoExternalReferenceLinkRepository;

	private final NeoCurationLinkRepository neoCurationLinkRepository;
	
	public NeoMessageBuffer(AgentNeo4JProperties properties, NeoSampleRepository neoSampleRepository,
			SampleToNeoSampleConverter sampleToNeoSampleConverter,
			NeoExternalReferenceLinkRepository neoExternalReferenceLinkRepository,
			NeoCurationLinkRepository neoCurationLinkRepository) {
		super(properties.getAgentNeo4JQueueSize(), properties.getAgentNeo4JQueueTime());
		this.neoSampleRepository = neoSampleRepository;
		this.sampleToNeoSampleConverter = sampleToNeoSampleConverter;
		this.neoExternalReferenceLinkRepository = neoExternalReferenceLinkRepository;
		this.neoCurationLinkRepository = neoCurationLinkRepository;
	}

	@Override
	@Transactional
	protected void save(Collection<MessageContent> messageContents) {		
		for (MessageContent messageContent : messageContents) {
			if (messageContent.hasSample()) {
				Sample sample = messageContent.getSample();
				NeoSample neoSample = sampleToNeoSampleConverter.convert(sample);
				neoSampleRepository.save(neoSample);
			}
			if (messageContent.hasExternalReferenceLink()) {
				ExternalReferenceLink externalRefrerenceLink = messageContent.getExternalReferenceLink();
				
				NeoExternalReference neoExternalReference = NeoExternalReference.build(externalRefrerenceLink.getUrl());
				NeoSample neoSample = neoSampleRepository.findOneByAccession(externalRefrerenceLink.getSample(), 0);	
				
				NeoExternalReferenceLink neoExternalReferenceLink = NeoExternalReferenceLink.build(neoExternalReference, neoSample);
				
				neoExternalReferenceLinkRepository.save(neoExternalReferenceLink);
			}
			if (messageContent.hasCurationLink()) {
				CurationLink curationLink = messageContent.getCurationLink();
				
				Collection<NeoAttribute> attributesPre = new ArrayList<>();
				Collection<NeoAttribute> attributesPost = new ArrayList<>();
				for (Attribute attribute : curationLink.getCuration().getAttributesPre()) {
					attributesPre.add(NeoAttribute.build(attribute.getType(), attribute.getValue(), attribute.getIri(), attribute.getUnit()));
				}
				for (Attribute attribute : curationLink.getCuration().getAttributesPost()) {
					attributesPost.add(NeoAttribute.build(attribute.getType(), attribute.getValue(), attribute.getIri(), attribute.getUnit()));
				}
				
				NeoCuration neoCuration = NeoCuration.build(attributesPre, attributesPost);
				NeoSample neoSample = neoSampleRepository.findOneByAccession(curationLink.getSample(), 0);
				
				NeoCurationLink neoCurationLink = NeoCurationLink.build(neoCuration, neoSample);
				
				neoCurationLinkRepository.save(neoCurationLink);
			}
		}
	}

}
