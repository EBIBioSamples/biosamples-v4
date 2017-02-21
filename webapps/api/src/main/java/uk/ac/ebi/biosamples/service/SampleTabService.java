package uk.ac.ebi.biosamples.service;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.SampleNode;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.CharacteristicAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.CommentAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.DatabaseAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.SCDNodeAttribute;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class SampleTabService {

	@Autowired
	private SampleService sampleService;
	
	public SampleData saveSampleTab(SampleData sampleData) {
		for (SampleNode sampleNode : sampleData.scd.getNodes(SampleNode.class)) {

			String accession = sampleNode.getSampleAccession();
			String name = sampleNode.getNodeName();
			LocalDateTime release = LocalDateTime.ofInstant(Instant.ofEpochMilli(sampleData.msi.submissionReleaseDate.getTime()), ZoneId.systemDefault());
			LocalDateTime update = LocalDateTime.ofInstant(Instant.ofEpochMilli(sampleData.msi.submissionUpdateDate.getTime()), ZoneId.systemDefault());

			SortedSet<Attribute> attributes = new TreeSet<>();
			SortedSet<Relationship> relationships = new TreeSet<>();
			
			for (SCDNodeAttribute attribute : sampleNode.attributes) {
				String type = attribute.getAttributeType();
				String value = attribute.getAttributeValue();
				URI iri = null;
				String unit = null;
				
				if (attribute instanceof CommentAttribute) {
					CommentAttribute commentAttribute = (CommentAttribute) attribute;
					
					type = commentAttribute.type;
					
					if (commentAttribute.unit != null 
							&& commentAttribute.unit.getAttributeValue() != null
							&& commentAttribute.unit.getAttributeValue().trim().length() > 0) {
						unit = commentAttribute.unit.getAttributeValue().trim();
					}
					
					//TODO query OLS to get the URI for a short form http://www.ebi.ac.uk/ols/api/terms?id=EFO_0000001
					
				} else if (attribute instanceof CharacteristicAttribute) {
					CharacteristicAttribute characteristicAttribute = (CharacteristicAttribute) attribute;
					
					type = characteristicAttribute.type;
					
					if (characteristicAttribute.unit != null 
							&& characteristicAttribute.unit.getAttributeValue() != null
							&& characteristicAttribute.unit.getAttributeValue().trim().length() > 0) {
						unit = characteristicAttribute.unit.getAttributeValue().trim();
					}
					
					
					//TODO query OLS to get the URI for a short form http://www.ebi.ac.uk/ols/api/terms?id=EFO_0000001	
				} else if (attribute instanceof DatabaseAttribute) {
					//TODO this is a data link, store appropriately
				}
				
				attributes.add(Attribute.build(type, value, iri, unit));
				
			}
			
			Sample sample = Sample.build(name, accession, release, update, attributes, relationships);
			sample = sampleService.store(sample);
			if (accession == null) {
				sampleNode.setSampleAccession(sample.getAccession());
			}
		}
		return sampleData;
	}
}
