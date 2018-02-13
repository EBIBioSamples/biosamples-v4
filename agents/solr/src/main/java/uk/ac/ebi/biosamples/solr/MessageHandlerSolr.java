package uk.ac.ebi.biosamples.solr;

import java.time.Instant;
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
		handleSample(sample, messageContent.getCreationTime());
		for (Sample related : messageContent.getRelated()) {
			handleSample(related, messageContent.getCreationTime());
		}
		
	}
	
	private void handleSample(Sample sample, String modifiedTime) {
		SolrSample solrSample = sampleToSolrSampleConverter.convert(sample);
		//add the modified time to the solrSample
		String indexedTime = ZonedDateTime.now(ZoneOffset.UTC)
				//.format(DateTimeFormatter.ofPattern("yyyy-mm-dd'T'HH:mm:ss.SSSX"));
				.format(DateTimeFormatter.ISO_INSTANT);
		
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
		
		//TODO expand by following relationships
		
		solrSample = repository.saveWithoutCommit(solrSample);

		log.trace("Handed "+sample.getAccession());
		
	}
}
