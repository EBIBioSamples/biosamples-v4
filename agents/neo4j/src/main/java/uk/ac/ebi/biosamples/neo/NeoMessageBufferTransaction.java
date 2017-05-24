package uk.ac.ebi.biosamples.neo;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.biosamples.MessageContent;
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
public class NeoMessageBufferTransaction {


	private final NeoSampleRepository neoSampleRepository;
	private final SampleToNeoSampleConverter sampleToNeoSampleConverter;

	private final NeoExternalReferenceRepository neoExternalReferenceRepository;
	private final NeoExternalReferenceLinkRepository neoExternalReferenceLinkRepository;

	private final NeoCurationRepository neoCurationRepository;
	private final NeoCurationLinkRepository neoCurationLinkRepository;
	
	public NeoMessageBufferTransaction(NeoSampleRepository neoSampleRepository,
			SampleToNeoSampleConverter sampleToNeoSampleConverter,
			NeoExternalReferenceRepository neoExternalReferenceRepository,
			NeoExternalReferenceLinkRepository neoExternalReferenceLinkRepository,
			NeoCurationRepository neoCurationRepository,
			NeoCurationLinkRepository neoCurationLinkRepository) {
		this.neoSampleRepository = neoSampleRepository;
		this.sampleToNeoSampleConverter = sampleToNeoSampleConverter;
		this.neoExternalReferenceRepository = neoExternalReferenceRepository;
		this.neoExternalReferenceLinkRepository = neoExternalReferenceLinkRepository;
		this.neoCurationRepository = neoCurationRepository;
		this.neoCurationLinkRepository = neoCurationLinkRepository;
	}
	
	
	@Transactional
	public void save(Collection<MessageContent> messageContents) {		
		for (MessageContent messageContent : messageContents) {
			if (messageContent.hasSample()) {
				Sample sample = messageContent.getSample();
				NeoSample neoSample = sampleToNeoSampleConverter.convert(sample);
				neoSampleRepository.save(neoSample);
			}
			if (messageContent.hasExternalReferenceLink()) {
				ExternalReferenceLink externalRefrerenceLink = messageContent.getExternalReferenceLink();
				
				NeoExternalReference neoExternalReference = NeoExternalReference.build(externalRefrerenceLink.getUrl());
				NeoSample neoSample = neoSampleRepository.findOneByAccession(externalRefrerenceLink.getSample(),1);	
				
				NeoExternalReferenceLink neoExternalReferenceLink = NeoExternalReferenceLink.build(neoExternalReference, neoSample);
				
				neoExternalReferenceRepository.save(neoExternalReference,0);
				neoExternalReferenceLinkRepository.save(neoExternalReferenceLink,1);
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
				NeoSample neoSample = neoSampleRepository.findOneByAccession(curationLink.getSample(),1);
				
				NeoCurationLink neoCurationLink = NeoCurationLink.build(neoCuration, neoSample);

				neoCurationRepository.save(neoCuration,1);
				neoCurationLinkRepository.save(neoCurationLink,1);
			}
		}
	}
}
