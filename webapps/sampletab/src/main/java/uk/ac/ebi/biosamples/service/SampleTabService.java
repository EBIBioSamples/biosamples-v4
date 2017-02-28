package uk.ac.ebi.biosamples.service;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.Node;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.GroupNode;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.SampleNode;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.AbstractRelationshipAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.CharacteristicAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.CommentAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.DatabaseAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.SCDNodeAttribute;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class SampleTabService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	private SubmissionService submissionService;

	public SampleTabService(SubmissionService submissionService) {
		this.submissionService = submissionService;
	}
	
	public SampleData saveSampleTab(SampleData sampleData) {
		for (SampleNode sampleNode : sampleData.scd.getNodes(SampleNode.class)) {
			String accession = sampleNode.getSampleAccession();
			String name = sampleNode.getNodeName();
			LocalDateTime release = LocalDateTime.ofInstant(Instant.ofEpochMilli(sampleData.msi.submissionReleaseDate.getTime()), ZoneId.systemDefault());
			LocalDateTime update = LocalDateTime.ofInstant(Instant.ofEpochMilli(sampleData.msi.submissionUpdateDate.getTime()), ZoneId.systemDefault());

			SortedSet<Attribute> attributes = new TreeSet<>();
			SortedSet<Relationship> relationships = new TreeSet<>();
			
			//beware, works by side-effect
			populateAttributes(accession, sampleNode.getAttributes(), attributes, relationships);
			
			//only build a sample if there is at least one attribute or it has no "parent" node
			//otherwise, it is just a group membership tracking dummy
			if (attributes.size() > 0 || sampleNode.getParentNodes().size() == 0) {			
				Sample sample = Sample.build(name, accession, release, update, attributes, relationships);
				sample = submissionService.submit(sample).getContent();
				if (accession == null) {
					sampleNode.setSampleAccession(sample.getAccession());
				}
			}
		}
		for (GroupNode groupNode : sampleData.scd.getNodes(GroupNode.class)) {
			String accession = groupNode.getGroupAccession();
			String name = groupNode.getNodeName();
			LocalDateTime release = LocalDateTime.ofInstant(Instant.ofEpochMilli(sampleData.msi.submissionReleaseDate.getTime()), ZoneId.systemDefault());
			LocalDateTime update = LocalDateTime.ofInstant(Instant.ofEpochMilli(sampleData.msi.submissionUpdateDate.getTime()), ZoneId.systemDefault());

			SortedSet<Attribute> attributes = new TreeSet<>();
			SortedSet<Relationship> relationships = new TreeSet<>();
			
			//beware, works by side-effect
			populateAttributes(accession, groupNode.getAttributes(), attributes, relationships);
				
			//add relationships from the group node to any member samples
			for (Node node : groupNode.getParentNodes()) {
				log.info("Found parent");
				if (node instanceof SampleNode) {
					SampleNode sampleNode = (SampleNode) node;
					relationships.add(Relationship.build("has member", sampleNode.getSampleAccession(), accession));
					log.info("Adding relationship from "+accession+" to "+sampleNode.getSampleAccession());
				}
			}		
			
			//this must be the last bit to build and save the object
			Sample sample = Sample.build(name, accession, release, update, attributes, relationships);
			sample = submissionService.submit(sample).getContent();
			if (accession == null) {
				groupNode.setGroupAccession(sample.getAccession());
			}				
		}
		return sampleData;
	}
	
	/**
	 * Works by side effect!
	 * 
	 * Converts the List<SCDNodeAttribute> into the passed SortedSet<Attribute> and sortedSet<Relationship> objects.
	 *   
	 * 
	 * @param scdNodeAttributes
	 */
	private void populateAttributes(String accession, List<SCDNodeAttribute> scdNodeAttributes, SortedSet<Attribute> attributes , SortedSet<Relationship> relationships) {		
		for (SCDNodeAttribute attribute : scdNodeAttributes) {
			String type = null;
			String value = null;
			URI iri = null;
			String unit = null;
			
			if (attribute instanceof CommentAttribute) {
				CommentAttribute commentAttribute = (CommentAttribute) attribute;					
				type = commentAttribute.type;
				value = commentAttribute.getAttributeValue();					
				if (commentAttribute.unit != null 
						&& commentAttribute.unit.getAttributeValue() != null
						&& commentAttribute.unit.getAttributeValue().trim().length() > 0) {
					unit = commentAttribute.unit.getAttributeValue().trim();
				}					
				String termSourceId = commentAttribute.getTermSourceID();					
				attributes.add(makeAttribute(type, value, termSourceId, unit));					
			} else if (attribute instanceof CharacteristicAttribute) {
				CharacteristicAttribute characteristicAttribute = (CharacteristicAttribute) attribute;					
				type = characteristicAttribute.type;
				value = characteristicAttribute.getAttributeValue();					
				if (characteristicAttribute.unit != null 
						&& characteristicAttribute.unit.getAttributeValue() != null
						&& characteristicAttribute.unit.getAttributeValue().trim().length() > 0) {
					unit = characteristicAttribute.unit.getAttributeValue().trim();
				}					
				String termSourceId = characteristicAttribute.getTermSourceID();					
				attributes.add(makeAttribute(type, value, termSourceId, unit));		
			} else if (attribute instanceof DatabaseAttribute) {
				//TODO this is a data link, store appropriately
			} else if (attribute instanceof AbstractRelationshipAttribute) {
				//this is a relationship, store appropriately
				AbstractRelationshipAttribute abstractRelationshipAttribute = (AbstractRelationshipAttribute) attribute;
				type = abstractRelationshipAttribute.getAttributeValue();
				value = abstractRelationshipAttribute.getAttributeValue();
				relationships.add(Relationship.build(type, value, accession));
			}				
		}
		
	}
	
	private Attribute makeAttribute(String type, String value, String termSourceId, String unit) {
		URI uri = null;
		if (termSourceId != null && termSourceId.trim().length() > 0) {
			//if we're given a full uri, use it
			try {
				uri = URI.create(termSourceId);
			} catch (IllegalArgumentException e) {
				//do nothing
			}
			if (uri == null) {
				//provided termSourceId wasnt a uri
				//TODO query OLS to get the URI for a short form http://www.ebi.ac.uk/ols/api/terms?id=EFO_0000001
			}
		}		
		return Attribute.build(type, value, uri, unit);
	}
}
