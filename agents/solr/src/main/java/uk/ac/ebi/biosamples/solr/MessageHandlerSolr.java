package uk.ac.ebi.biosamples.solr;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.ols.OlsProcessor;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;
import uk.ac.ebi.biosamples.solr.service.SampleToSolrSampleConverter;

@Service
public class MessageHandlerSolr {
	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private SolrSampleRepository repository;
	
	@Autowired
	private SampleToSolrSampleConverter sampleToSolrSampleConverter;
	
	@Autowired
	private OlsProcessor olsProcessor;
		
	@RabbitListener(queues = Messaging.queueToBeIndexedSolr)
	public void handle(MessageContent messageContent) throws Exception {
		
		if (messageContent.getSample() == null) {
			log.warn("Recieved message without sample");
			return;
		}

		Sample sample = messageContent.getSample();
		SolrSample solrSample = sampleToSolrSampleConverter.convert(sample);
		//add the modified time to the solrSample
		String modifiedTime = messageContent.getCreationTime();
		String indexedTime = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
		
		SolrSample oldSolrSample = repository.findOne(sample.getAccession());
		if (oldSolrSample != null) {
			//there was an old sample
			//check it was modified before us
			if (ZonedDateTime.parse(oldSolrSample.getModified(), DateTimeFormatter.ISO_INSTANT)
					.isBefore(ZonedDateTime.parse(oldSolrSample.getModified(), DateTimeFormatter.ISO_INSTANT))) {
				throw new AmqpRejectAndDontRequeueException("Replaced by newer message");
			}
		}
				
		solrSample = SolrSample.build(solrSample.getName(), solrSample.getAccession(), solrSample.getDomain(), 
				solrSample.getRelease(), solrSample.getUpdate(),
				modifiedTime, indexedTime, 
				solrSample.getAttributeValues(), solrSample.getAttributeIris(), solrSample.getAttributeUnits(), 
				solrSample.getOutgoingRelationships(), solrSample.getIncomingRelationships(), 
				solrSample.getExternalReferencesData(), solrSample.getKeywords());
		
		//expand ontology terms from OLS
		for (List<String> iris : solrSample.getAttributeIris().values()) {
			for (String iri : iris) {
				solrSample.getKeywords().addAll(olsProcessor.ancestorsAndSynonyms("efo", iri));
				solrSample.getKeywords().addAll(olsProcessor.ancestorsAndSynonyms("NCBITaxon", iri));
			}
		}
		
		//expand by following relationships
		for (Sample relatedSample : messageContent.getRelated()) {
			solrSample.getKeywords().add(relatedSample.getAccession());
			solrSample.getKeywords().add(relatedSample.getName());
			for (Attribute attribute : relatedSample.getAttributes()) {
				solrSample.getKeywords().add(attribute.getType());
				solrSample.getKeywords().add(attribute.getValue());
				if (attribute.getUnit() != null) {
					solrSample.getKeywords().add(attribute.getUnit());
				}
				if (attribute.getIri().size() > 0) {
					for (String iri : attribute.getIri()) {
						//expand ontology terms of related samples against ols
						solrSample.getKeywords().addAll(olsProcessor.ancestorsAndSynonyms("efo", iri));
					}
				}
			}
		}
		
		solrSample = repository.saveWithoutCommit(solrSample);

		log.info("Handed "+sample.getAccession());
		
	}
}
