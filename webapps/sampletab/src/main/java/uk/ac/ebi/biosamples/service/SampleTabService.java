package uk.ac.ebi.biosamples.service;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Service;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.Node;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.GroupNode;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.SampleNode;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.AbstractNamedAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.AbstractRelationshipAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.CharacteristicAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.CommentAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.DatabaseAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.SCDNodeAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.renderer.SampleTabWriter;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.mongo.model.MongoSampleTab;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleTabRepository;

@Service
public class SampleTabService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	private final BioSamplesClient bioSamplesClient;
	private final MongoSampleTabRepository mongoSampleTabRepository;
	private final SampleTabIdService sampleTabIdSerivce;

	public SampleTabService(BioSamplesClient bioSamplesClient, MongoSampleTabRepository mongoSampleTabRepository, SampleTabIdService sampleTabIdService) {
		this.bioSamplesClient = bioSamplesClient;
		this.mongoSampleTabRepository = mongoSampleTabRepository;
		this.sampleTabIdSerivce = sampleTabIdService;
	}
	public SampleData accessionSampleTab(SampleData sampleData, String domain, String jwt, boolean setUpdateDate) 
			throws DuplicateDomainSampleException {

		//release in 100 years time
		Instant release = Instant.ofEpochMilli(LocalDateTime.now(ZoneOffset.UTC).plusYears(100).toEpochSecond(ZoneOffset.UTC));
		Instant update = Instant.ofEpochMilli(sampleData.msi.submissionUpdateDate.getTime());
		for (SampleNode sampleNode : sampleData.scd.getNodes(SampleNode.class)) {
			String accession = sampleNode.getSampleAccession();
			String name = sampleNode.getNodeName();
			
			if (accession != null) {
				continue;
			}
			
			//only build a sample if there is at least one attribute or it has no "parent" node
			//otherwise, it is just a group membership tracking dummy
			if (sampleNode.getAttributes().size() > 0 || sampleNode.getChildNodes().size() == 0) {
				//find any existing samples in the same domain with the same name
				
				if (sampleNode.getSampleAccession() == null) {
					//if there was no accession provided, try to find an existing accession by name and domain
					List<Filter> filterList = new ArrayList<>(2);
					filterList.add(FilterBuilder.create().onName(name).build());
					filterList.add(FilterBuilder.create().onDomain(domain).build());
					Iterator<Resource<Sample>> it = bioSamplesClient.fetchSampleResourceAll(null, filterList).iterator();
	
					Resource<Sample> first = null;
					if (it.hasNext()) {
						first = it.next();
						if (it.hasNext()) {
							//error multiple accessions
							throw new DuplicateDomainSampleException(domain, name);
						} else {
							accession = first.getContent().getAccession();
							sampleNode.setSampleAccession(accession);
						}
					}
				}
				
				Sample sample = Sample.build(name, accession, domain, release, update, new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
				sample = bioSamplesClient.persistSampleResource(sample, setUpdateDate).getContent();
				if (sampleNode.getSampleAccession() == null) {
					sampleNode.setSampleAccession(sample.getAccession());
				}
			}
		}
		for (GroupNode groupNode : sampleData.scd.getNodes(GroupNode.class)) {
			String accession = groupNode.getGroupAccession();
			String name = groupNode.getNodeName();
			
			//TODO decide how to handle new groups
							
			//this must be the last bit to build and save the object
			Sample sample = Sample.build(name, accession, domain, release, update, new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
			sample = bioSamplesClient.persistSampleResource(sample, setUpdateDate).getContent();
			if (accession == null) {
				groupNode.setGroupAccession(sample.getAccession());
			}				
		}
		
		return sampleData;
	}
	
	public SampleData saveSampleTab(SampleData sampleData, String domain, String jwt, boolean setUpdateDate) 
			throws DuplicateDomainSampleException, ConflictingSampleTabOwnershipException {
		
		Instant release = Instant.ofEpochMilli(sampleData.msi.submissionReleaseDate.getTime());
		Instant update = Instant.ofEpochMilli(sampleData.msi.submissionUpdateDate.getTime());

		Map<String, Future<Resource<Sample>>> futureMap = new TreeMap<>();
		for (SampleNode sampleNode : sampleData.scd.getNodes(SampleNode.class)) {
			String accession = sampleNode.getSampleAccession();
			String name = sampleNode.getNodeName();

			SortedSet<Attribute> attributes = new TreeSet<>();
			SortedSet<Relationship> relationships = new TreeSet<>();
			SortedSet<ExternalReference> externalReferences = new TreeSet<>();
			
			//beware, works by side-effect
			populateAttributes(accession, sampleNode.getAttributes(), attributes, relationships, externalReferences);
			
			if (sampleNode.getSampleDescription() != null && 
					sampleNode.getSampleDescription().trim().length() > 0) {
				attributes.add(Attribute.build("description", sampleNode.getSampleDescription()));
			}			
			
			//only build a sample if there is at least one attribute or it has no "parent" node
			//otherwise, it is just a group membership tracking dummy		
			if (attributes.size()+relationships.size()+externalReferences.size() > 0
					|| sampleNode.getChildNodes().size() == 0) {

				if (sampleNode.getSampleAccession() == null) {
					//if there was no accession provided, try to find an existing accession by name and domain
					List<Filter> filterList = new ArrayList<>(2);
					filterList.add(FilterBuilder.create().onName(name).build());
					filterList.add(FilterBuilder.create().onDomain(domain).build());
					Iterator<Resource<Sample>> it = bioSamplesClient.fetchSampleResourceAll(null, filterList).iterator();
	
					Resource<Sample> first = null;
					if (it.hasNext()) {
						first = it.next();
						if (it.hasNext()) {
							//error multiple accessions
							throw new DuplicateDomainSampleException(domain, name);
						} else {
							accession = first.getContent().getAccession();
							sampleNode.setSampleAccession(accession);
						}
					}
				}
				
				Sample sample = Sample.build(name, accession, domain, release, update, attributes, relationships, externalReferences);
				futureMap.put(name, bioSamplesClient.persistSampleResourceAsync(sample, setUpdateDate));	
			}			
		}

		//resolve futures for submitting samples
		for (String futureName : futureMap.keySet()) {
			Sample sample;
			try {
				sample = futureMap.get(futureName).get().getContent();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
			SampleNode sampleNode = sampleData.scd.getNode(futureName, SampleNode.class);
			//if it didn't have an accession before, assign one now
			if (sampleNode.getSampleAccession() == null) {
				sampleNode.setSampleAccession(sample.getAccession());
			}
		}
		
		for (GroupNode groupNode : sampleData.scd.getNodes(GroupNode.class)) {
			String accession = groupNode.getGroupAccession();
			String name = groupNode.getNodeName();

			SortedSet<Attribute> attributes = new TreeSet<>();
			SortedSet<Relationship> relationships = new TreeSet<>();
			SortedSet<ExternalReference> externalReferences = new TreeSet<>();
			
			//beware, works by side-effect
			populateAttributes(accession, groupNode.getAttributes(), attributes, relationships, externalReferences);
				
			//add relationships from the group node to any member samples
			for (Node node : groupNode.getParentNodes()) {
				//log.info("Found parent");
				if (node instanceof SampleNode) {
					SampleNode sampleNode = (SampleNode) node;
					relationships.add(Relationship.build(accession, "has member", sampleNode.getSampleAccession()));
					//log.info("Adding relationship from "+accession+" to "+sampleNode.getSampleAccession());
				}
			}		
			
			//this must be the last bit to build and save the object
			Sample sample = Sample.build(name, accession, domain, release, update, attributes, relationships, externalReferences);
			sample = bioSamplesClient.persistSampleResource(sample, setUpdateDate).getContent();
			if (accession == null) {
				groupNode.setGroupAccession(sample.getAccession());
			}				
		}
		
		
		if (sampleData.msi.submissionIdentifier != null) {
			//this is an update of an existing sampletab
			//get old sampletab document	
			MongoSampleTab oldSampleTab = mongoSampleTabRepository.findOne(sampleData.msi.submissionIdentifier);
			
			if (oldSampleTab == null) {
				//no previous submission with this Id
				//TODO if user is not super-user, abort
				
			} else {
			
				//delete any samples/groups that were in the old version but no the latest one
				Set<String> oldAccessions = new HashSet<>(oldSampleTab.getAccessions());
				Set<String> newAccessions = new HashSet<>();
				for (SampleNode sampleNode : sampleData.scd.getNodes(SampleNode.class)) {
					newAccessions.add(sampleNode.getSampleAccession());
				}
				for (GroupNode groupNode : sampleData.scd.getNodes(GroupNode.class)) {
					newAccessions.add(groupNode.getGroupAccession());
				}
				oldAccessions.removeAll(newAccessions);
				for (String toRemove : oldAccessions) {
					//get the existing version to be deleted
					Optional<Resource<Sample>> oldSample = bioSamplesClient.fetchSampleResource(toRemove);
					if (oldSample.isPresent()) {
						//don't do a hard-delete, instead mark it as public in 100 years
						Sample sample = Sample.build(oldSample.get().getContent().getName(), toRemove, domain, 
								ZonedDateTime.now(ZoneOffset.UTC).plusYears(100).toInstant(), update, 
								new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
						sample = bioSamplesClient.persistSampleResource(sample, setUpdateDate).getContent();
					}
				}
				
				//check samples are owned by this sampletab and not any others
				for (String accession : newAccessions) {
					MongoSampleTab accessionSampleTab = mongoSampleTabRepository.findOneByAccessionContaining(accession);
					if (accessionSampleTab != null && !accessionSampleTab.getId().equals(sampleData.msi.submissionIdentifier)) {
						//TODO this sample is "owned" by a different sampletab file
						
						throw new ConflictingSampleTabOwnershipException(accession, accessionSampleTab.getId(), sampleData.msi.submissionIdentifier);
					}
				}
			}
			

			//persist latest SampleTab
			//get the accessions in it
			Set<String> sampletabAccessions = new HashSet<>();
			for (SampleNode sampleNode : sampleData.scd.getNodes(SampleNode.class)) {
				sampletabAccessions.add(sampleNode.getSampleAccession());
			}
			for (GroupNode groupNode : sampleData.scd.getNodes(GroupNode.class)) {
				sampletabAccessions.add(groupNode.getGroupAccession());
			}			
			//write the sampledata object into a string representation
			//this might end up being slightly different from what was submitted
			//so we still need to keep the original POST content
			SampleTabWriter sampleTabWriter = null;
			StringWriter stringWriter = null;
			String sampleTab = null;
			try {
				stringWriter = new StringWriter();
				sampleTabWriter = new SampleTabWriter(stringWriter);
				sampleTab = stringWriter.toString();
			} finally {
				if (sampleTabWriter != null) {
					try {
						sampleTabWriter.close();
					} catch (IOException e) {
						//do nothing
					}
				}
				if (stringWriter != null) {
					try {
						stringWriter.close();
					} catch (IOException e) {
						//do nothing
					}
				}
			}
			//actually persist it
			mongoSampleTabRepository.save(MongoSampleTab.build(sampleData.msi.submissionIdentifier, domain, sampleTab, sampletabAccessions));
			
		} else {
			//no existing sample tab identifier
			//submit to generate one, then return it to client
			//TODO check samples are not owned by other sampletabs
			//TODO persist latest SampleTab
		}
		
		
		
		
		return sampleData;
	}
	
	/**
	 * Works by side effect!
	 * 
	 * Converts the List<SCDNodeAttribute> into the passed SortedSet objects.
	 *   
	 * 
	 * @param scdNodeAttributes
	 */
	private void populateAttributes(String accession, List<SCDNodeAttribute> scdNodeAttributes, 
			SortedSet<Attribute> attributes , SortedSet<Relationship> relationships, SortedSet<ExternalReference> externalReferences) {		
		for (SCDNodeAttribute attribute : scdNodeAttributes) {
			String type = null;
			String value = null;
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

			} else if (attribute instanceof AbstractNamedAttribute) {
				AbstractNamedAttribute abstractNamedAttribute = (AbstractNamedAttribute) attribute;		
				type = abstractNamedAttribute.getAttributeType();
				value = abstractNamedAttribute.getAttributeValue();	
				String termSourceId = abstractNamedAttribute.getTermSourceID();					
				attributes.add(makeAttribute(type, value, termSourceId, null));				
			} else if (attribute instanceof DatabaseAttribute) {
				DatabaseAttribute databaseAttribute = (DatabaseAttribute) attribute;
				if (databaseAttribute.databaseURI != null) {
					externalReferences.add(ExternalReference.build(databaseAttribute.databaseURI));
				}				
			} else if (attribute instanceof AbstractRelationshipAttribute) {
				//this is a relationship, store appropriately
				AbstractRelationshipAttribute abstractRelationshipAttribute = (AbstractRelationshipAttribute) attribute;
				type = abstractRelationshipAttribute.getAttributeType().toLowerCase();
				value = abstractRelationshipAttribute.getAttributeValue();
				relationships.add(Relationship.build(accession, type, value));
			}				
		}		
	}
	
	private Attribute makeAttribute(String type, String value, String termSourceId, String unit) {
		String uri = null;
		if (termSourceId != null && termSourceId.trim().length() > 0) {
			//if we're given a full uri, use it
			try {
				uri = termSourceId;
			} catch (IllegalArgumentException e) {
				//do nothing
			}
			if (uri == null) {
				//provided termSourceId wasn't a uri
				//TODO query OLS to get the URI for a short form http://www.ebi.ac.uk/ols/api/terms?id=EFO_0000001
			}
		}		
		return Attribute.build(type, value, uri, unit);
	}
	
	public static class DuplicateDomainSampleException extends Exception {
		
		private static final long serialVersionUID = -3469688972274912777L;
		public final String domain;
		public final String name;
		
		public DuplicateDomainSampleException(String domain, String name) {
			super("Multiple existing accessions of domain '"+domain+"' sample name '"+name+"'");
			this.domain = domain;
			this.name = name;
		}
	}
	
	public static class ConflictingSampleTabOwnershipException extends Exception {
		
		private static final long serialVersionUID = -1504945560846665587L;
		public final String sampleAccession;
		public final String originalSubmission;
		public final String newSubmission;
		
		public ConflictingSampleTabOwnershipException (String sampleAccession, String originalSubmission, String newSubmission) {
			super("Accession "+sampleAccession+" was previouly described in "+originalSubmission);
			this.sampleAccession = sampleAccession;
			this.originalSubmission = originalSubmission;
			this.newSubmission = newSubmission;
		}
	}
}
