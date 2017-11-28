package uk.ac.ebi.biosamples.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Service;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.Node;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.MSI;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.GroupNode;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.SampleNode;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.*;
import uk.ac.ebi.arrayexpress2.sampletab.renderer.SampleTabWriter;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.mongo.model.MongoSampleTab;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleTabRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoAccessionService;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class SampleTabService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	private final BioSamplesClient bioSamplesClient;
	private final MongoSampleTabRepository mongoSampleTabRepository;
	private final SampleTabIdService sampleTabIdSerivce;
	private final MongoAccessionService mongoGroupAccessionService;

	public SampleTabService(BioSamplesClient bioSamplesClient, MongoSampleTabRepository mongoSampleTabRepository,
			SampleTabIdService sampleTabIdService, @Qualifier("mongoGroupAccessionService") MongoAccessionService mongoGroupAccessionService) {
		this.bioSamplesClient = bioSamplesClient;
		this.mongoSampleTabRepository = mongoSampleTabRepository;
		this.sampleTabIdSerivce = sampleTabIdService;
		this.mongoGroupAccessionService = mongoGroupAccessionService;
	}
	
	public SampleData accessionSampleTab(SampleData sampleData, String domain, String jwt, boolean setUpdateDate) 
			throws DuplicateDomainSampleException {
		
		log.trace("Accessioning sampletab "+sampleData.msi.submissionIdentifier);

		//release in 100 years time
		Instant release = Instant.ofEpochMilli(LocalDateTime.now(ZoneOffset.UTC).plusYears(100).toEpochSecond(ZoneOffset.UTC));
		Instant update = Instant.ofEpochMilli(sampleData.msi.submissionUpdateDate.getTime());

		//put any existing accessions into the samplenode and groupnode objects
		populateExistingAccessions(sampleData, domain, release, update);		

		//because we are only accessioning samples and groups, we don't care about submission ownership
				
		//persist the samples and groups
		//TODO only persist unaccessioned?
		persistSamplesAndGroups(sampleData, domain, release, update, setUpdateDate);
		
		//because we are only accessioning samples and groups, do not persist sampletab itself
		
		return sampleData;
	}
	
	public SampleData saveSampleTab(SampleData sampleData, String domain, boolean superUser, boolean setUpdateDate, boolean setFullDetails)
			throws DuplicateDomainSampleException, ConflictingSampleTabOwnershipException, AssertingSampleTabOwnershipException {
		
		log.info("Saving sampletab "+sampleData.msi.submissionIdentifier);
		
		Instant release = Instant.ofEpochMilli(sampleData.msi.submissionReleaseDate.getTime());
		Instant update = Instant.ofEpochMilli(sampleData.msi.submissionUpdateDate.getTime());

		//put any existing accessions into the samplenode and groupnode objects
		populateExistingAccessions(sampleData, domain, release, update);	

		MongoSampleTab oldSampleTab = null;

        //replace implicit derived from with explicit derived from relationships
        for (SampleNode sample : sampleData.scd.getNodes(SampleNode.class)) {
            if (sample.getParentNodes().size() > 0) {
                for (Node parent : new HashSet<Node>(sample.getParentNodes())) {
                    if (SampleNode.class.isInstance(parent)) {
                        SampleNode parentsample = (SampleNode) parent;
                        DerivedFromAttribute attr = new DerivedFromAttribute(parentsample.getSampleAccession());
                        sample.addAttribute(attr);
                        sample.removeParentNode(parentsample);
                        parentsample.removeChildNode(sample);
                    }
                }
            }
        }

		Set<String> newAccessions = new HashSet<>();
		for (SampleNode sampleNode : sampleData.scd.getNodes(SampleNode.class)) {
			if (!isDummy(sampleNode)) {
				if (sampleNode.getSampleAccession() != null && sampleNode.getSampleAccession().trim().length() > 0) {
					newAccessions.add(sampleNode.getSampleAccession());
				}
			}
		}
		for (GroupNode groupNode : sampleData.scd.getNodes(GroupNode.class)) {
			if (groupNode.getGroupAccession() != null && groupNode.getGroupAccession().trim().length() > 0) {
				newAccessions.add(groupNode.getGroupAccession());
			}
		}
		
		if (sampleData.msi.submissionIdentifier != null 
				&& sampleData.msi.submissionIdentifier.trim().length() > 0) {
			//this is an update of an existing sampletab
			//get old sampletab document	
			oldSampleTab = mongoSampleTabRepository.findOne(sampleData.msi.submissionIdentifier);

			
			if (oldSampleTab == null) {
				//no previous submission with this Id
				//if user is not super-user, abort
				if (!superUser) {					
					throw new AssertingSampleTabOwnershipException(sampleData.msi.submissionIdentifier); 					
				}

				//check samples are not owned by any others
				for (String accession : newAccessions) {
					List<MongoSampleTab> accessionSampleTabs = mongoSampleTabRepository.findOneByAccessionContaining(accession);

					String newId = sampleData.msi.submissionIdentifier.trim();
					
					if (accessionSampleTabs == null) {
						log.info("Null accession sample tabs for accession "+accession);
					} else if (accessionSampleTabs.size() == 0) {
						log.info("No accession sample tabs for accession "+accession);
					} else if (accessionSampleTabs.size() > 1) {
						log.info("Multiple accession sample tabs for accession "+accession);	
						MongoSampleTab accessionSampleTab = accessionSampleTabs.get(0);
						String existingId = accessionSampleTab.getId().trim();			
						throw new ConflictingSampleTabOwnershipException(accession, existingId, newId);
					} else if (accessionSampleTabs.size() == 1) {
						log.info("One accession sample tabs for accession "+accession);
						MongoSampleTab accessionSampleTab = accessionSampleTabs.get(0);
						String existingId = accessionSampleTab.getId().trim();
						log.info("existingId = "+existingId);
						log.info("newId = "+newId);
						if (!existingId.equals(newId)) {
							//this sample is "owned" by a different sampletab file						
							throw new ConflictingSampleTabOwnershipException(accession, 
									existingId, newId);
						}
					}
				}
			} else {
				Set<String> oldAccessions = new HashSet<>(oldSampleTab.getAccessions());
				oldAccessions.removeAll(newAccessions);
				
				//check samples are owned by this sampletab and not any others
				for (String accession : newAccessions) {
					List<MongoSampleTab> accessionSampleTabs = mongoSampleTabRepository.findOneByAccessionContaining(accession);

					String newId = sampleData.msi.submissionIdentifier.trim();
					
					if (accessionSampleTabs == null) {
						log.info("Null accession sample tabs for accession "+accession);
					} else if (accessionSampleTabs.size() == 0) {
						log.info("No accession sample tabs for accession "+accession);
					} else if (accessionSampleTabs.size() > 1) {
						log.info("Multiple accession sample tabs for accession "+accession);
						MongoSampleTab accessionSampleTab = accessionSampleTabs.get(0);
						String existingId = accessionSampleTab.getId().trim();	
						throw new ConflictingSampleTabOwnershipException(accession, 
								existingId, newId);
					} else if (accessionSampleTabs.size() == 1) {
						log.info("One accession sample tabs for accession "+accession);
						MongoSampleTab accessionSampleTab = accessionSampleTabs.get(0);
						String existingId = accessionSampleTab.getId().trim();
						log.info("existingId = "+existingId);
						log.info("newId = "+newId);
						if (!existingId.equals(newId)) {
							//this sample is "owned" by a different sampletab file						
							throw new ConflictingSampleTabOwnershipException(accession, 
									existingId, newId);
						}
					}
				}
			
				//delete any samples/groups that were in the old version but not the latest one
				for (String toRemove : oldAccessions) {
					//get the existing version to be deleted
					Optional<Resource<Sample>> oldSample = bioSamplesClient.fetchSampleResource(toRemove);
					if (oldSample.isPresent()) {
						//don't do a hard-delete, instead mark it as public in 100 years
						Sample sample = Sample.build(oldSample.get().getContent().getName(), toRemove, domain, 
								ZonedDateTime.now(ZoneOffset.UTC).plusYears(100).toInstant(), update, 
								new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
						sample = bioSamplesClient.persistSampleResource(sample, setUpdateDate,true ).getContent();
					}
				}
				
			}			
		} else {
			//no previous sampletab submission id
			//persist latest SampleTab so it gets a submission id
			persistSampleTab(sampleData, domain);
			
			//check samples are not owned by any others
			for (String accession : newAccessions) {
				List<MongoSampleTab> accessionSampleTabs = mongoSampleTabRepository.findOneByAccessionContaining(accession);

				String newId = sampleData.msi.submissionIdentifier.trim();
				
				if (accessionSampleTabs == null) {
					log.info("Null accession sample tabs for accession "+accession);
				} else if (accessionSampleTabs.size() == 0) {
					log.info("No accession sample tabs for accession "+accession);
				} else if (accessionSampleTabs.size() > 1) {
					log.info("Multiple accession sample tabs for accession "+accession);	
					MongoSampleTab accessionSampleTab = accessionSampleTabs.get(0);
					String existingId = accessionSampleTab.getId().trim();			
					throw new ConflictingSampleTabOwnershipException(accession, existingId, newId);
				} else if (accessionSampleTabs.size() == 1) {
					log.info("One accession sample tabs for accession "+accession);
					MongoSampleTab accessionSampleTab = accessionSampleTabs.get(0);
					String existingId = accessionSampleTab.getId().trim();
					log.info("existingId = "+existingId);
					log.info("newId = "+newId);
					if (!existingId.equals(newId)) {
						//this sample is "owned" by a different sampletab file						
						throw new ConflictingSampleTabOwnershipException(accession, 
								existingId, newId);
					}
				}
			}
			
		}
		
		//at this point, sampleData must have a sane submissionIdentifier
		if (sampleData.msi.submissionIdentifier == null || sampleData.msi.submissionIdentifier.trim().length() == 0) {
			throw new RuntimeException("Failed to find submission identifier");
		}

        if (sampleData.scd.getNodes(GroupNode.class).size() == 0) {
        	// TODO need to check submissionIdentifier is popualted correctly
            GroupNode othergroup = new GroupNode("Submission "+sampleData.msi.submissionIdentifier);
            for (SampleNode sample : sampleData.scd.getNodes(SampleNode.class)) {
                // check there is not an existing group first...
                boolean sampleInGroup = false;
                //even if it has child nodes, both parent and child must be in a group
                //this will lead to some weird looking row duplications, but since this is an internal
                //intermediate file it is not important
                //Follow up: since implicit derived from relationships are made explicit above,
                //this is not an issue any more
                for (Node n : sample.getChildNodes()) {
                   if (GroupNode.class.isInstance(n)) {
                        sampleInGroup = true;
                    }
                }

                if (!sampleInGroup){
                    log.info("Adding sample " + sample.getNodeName() + " to group " + othergroup.getNodeName());
                    othergroup.addSample(sample);
                }
            }
            //only add the new group if it has any samples
            if (othergroup.getParentNodes().size() > 0){
            	try {
					sampleData.scd.addNode(othergroup);
				} catch (ParseException e) {
					//this should never happen
					throw new RuntimeException(e);
				}
                log.info("Added group node \""+othergroup.getNodeName()+"\"");
                // also need to accession the new node
            }
        }


		//persist the samples and groups
		persistSamplesAndGroups(sampleData, domain, release, update, setUpdateDate);

		//persist updated SampleTab so has all the associated accessions added
		persistSampleTab(sampleData, domain);
		
		return sampleData;
	}
	
	private void persistSampleTab(SampleData sampleData, String domain) {
		//get the accessions in it
		Set<String> sampletabAccessions = new HashSet<>();
		for (SampleNode sampleNode : sampleData.scd.getNodes(SampleNode.class)) {
			//don't associate accessions that belong to relationship tracking nodes 
			if (!isDummy(sampleNode)) {
				sampletabAccessions.add(sampleNode.getSampleAccession());
			}
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
			sampleTabWriter.write(sampleData);
			sampleTab = stringWriter.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
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
		//this will assign a new submission identifier if needed
		MongoSampleTab mongoSampleTab = MongoSampleTab.build(sampleData.msi.submissionIdentifier, domain, sampleTab, sampletabAccessions);
		if (mongoSampleTab.getId() == null || mongoSampleTab.getId().length()==0) {
			log.info("Generating new sampletab identifier");
			mongoSampleTab = sampleTabIdSerivce.accessionAndInsert(mongoSampleTab);
			sampleData.msi.submissionIdentifier = mongoSampleTab.getId();
			log.info("Generated new sampletab identifier "+mongoSampleTab.getId());
		} else {			
			log.info("Saving submission "+sampleData.msi.submissionIdentifier);
			mongoSampleTab = mongoSampleTabRepository.save(mongoSampleTab);
		}
		
	}
	
	/**
	 * This will save each individual sample and group in the sampletab file
	 * If they don't have accessions before, new ones will be assigned *AND STORED IN sampleData* 
	 * Note THIS WORKS BY SIDE-EFFECT
	 * 
	 * @param sampleData
	 * @param domain
	 * @param release
	 * @param update
	 * @param setUpdateDate
	 */
	private void persistSamplesAndGroups(SampleData sampleData, String domain, Instant release, Instant update, boolean setUpdateDate) {		
		Map<String, Future<Resource<Sample>>> futureMap = new TreeMap<>();
		for (SampleNode sampleNode : sampleData.scd.getNodes(SampleNode.class)) {
			if (!isDummy(sampleNode)) {			
				Sample sample = sampleNodeToSample(sampleNode, sampleData.msi, domain, release, update);
				futureMap.put(sample.getName(), bioSamplesClient.persistSampleResourceAsync(sample, setUpdateDate, true));
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
			Sample sample = groupNodeToSample(groupNode, sampleData.msi, domain, release, update);
			//assign this fake sample a group accession *before* it is actually persisted
			//and make sure it is a group accession
			if (sample.getAccession() == null || sample.getAccession().trim().length() == 0) {
				sample = mongoGroupAccessionService.generateAccession(sample);
			}

			//add "has member" relationship to "parent" nodes on the left in the MSI
			Collection<Relationship> relationships = Sets.newCopyOnWriteArraySet(sample.getRelationships());
			for (Node parent : groupNode.getParentNodes()) {
				if (SampleNode.class.isInstance(parent)) {
					SampleNode sampleNode = (SampleNode) parent;
					relationships.add(Relationship.build(sample.getAccession(), "has member", sampleNode.getSampleAccession()));
				}
			}
			sample = Sample.build(sample.getName(), sample.getAccession(), sample.getDomain(),
					sample.getRelease(), sample.getUpdate(), sample.getAttributes(), relationships, sample.getExternalReferences(),
					sample.getOrganizations(), sample.getContacts(), sample.getPublications());

			sample = bioSamplesClient.persistSampleResource(sample, setUpdateDate, true).getContent();
			if (groupNode.getGroupAccession() == null) {
				groupNode.setGroupAccession(sample.getAccession());
			}				
		}
	}

	//if there is at least one attribute or it has no "parent" node then it might be a real sample
	//otherwise, it is just a group membership tracking dummy
	private boolean isDummy(SampleNode sampleNode) {
		if (sampleNode.getAttributes().size() > 0
				|| (sampleNode.getSampleDescription() != null && sampleNode.getSampleDescription().trim().length() > 0)
				|| sampleNode.getChildNodes().size() == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	
	
	private Sample groupNodeToSample(GroupNode groupNode, MSI msi, String domain, Instant release, Instant update) {

		String accession = groupNode.getGroupAccession();
		String name = groupNode.getNodeName();

		SortedSet<Attribute> attributes = new TreeSet<>();
		SortedSet<Relationship> relationships = new TreeSet<>();
		SortedSet<ExternalReference> externalReferences = new TreeSet<>();
		SortedSet<Organization> organizations = getOrganizationsFromMSI(msi);
		SortedSet<Contact> contacts = getContactsFromMSI(msi);
		SortedSet<Publication> publications = getPublicationsFromMSI(msi);


		//beware, works by side-effect
		populateAttributes(accession, groupNode.getAttributes(), attributes, relationships, externalReferences);
		
		if (groupNode.getGroupDescription() != null && 
				groupNode.getGroupDescription().trim().length() > 0) {
			attributes.add(Attribute.build("description", groupNode.getGroupDescription()));
		}			
		//add submission information
		attributes.add(Attribute.build("Submission identifier", msi.submissionIdentifier));
		if (msi.submissionDescription != null && msi.submissionDescription.trim().length() > 0) {
			attributes.add(Attribute.build("Submission description", msi.submissionDescription));			
		}
		if (msi.submissionTitle != null && msi.submissionTitle.trim().length() > 0) {
			attributes.add(Attribute.build("Submission title", msi.submissionTitle));			
		}
		Sample sample = Sample.build(name, accession, domain, release, update,
				attributes, relationships, externalReferences,
				organizations, contacts, publications);

		return sample;
	}
	
	private Sample sampleNodeToSample(SampleNode sampleNode, MSI msi, String domain, Instant release, Instant update) {

		String accession = sampleNode.getSampleAccession();
		String name = sampleNode.getNodeName();

		SortedSet<Attribute> attributes = new TreeSet<>();
		SortedSet<Relationship> relationships = new TreeSet<>();
		SortedSet<ExternalReference> externalReferences = new TreeSet<>();
		SortedSet<Organization> organizations = getOrganizationsFromMSI(msi);
		SortedSet<Contact> contacts = getContactsFromMSI(msi);
		SortedSet<Publication> publications = getPublicationsFromMSI(msi);

		//beware, works by side-effect
		populateAttributes(accession, sampleNode.getAttributes(), attributes, relationships, externalReferences);
		
		//add description
		if (sampleNode.getSampleDescription() != null && 
				sampleNode.getSampleDescription().trim().length() > 0) {
			attributes.add(Attribute.build("description", sampleNode.getSampleDescription()));
		}
		//add submission information
		attributes.add(Attribute.build("Submission identifier", msi.submissionIdentifier));
		if (msi.submissionDescription != null && msi.submissionDescription.trim().length() > 0) {
			attributes.add(Attribute.build("Submission description", msi.submissionDescription));			
		}
		if (msi.submissionTitle != null && msi.submissionTitle.trim().length() > 0) {
			attributes.add(Attribute.build("Submission title", msi.submissionTitle));			
		}

		Sample sample = Sample.build(name, accession, domain, release, update,
				attributes, relationships, externalReferences,
				organizations, contacts, publications);
		return sample;
	}

	private SortedSet<Organization> getOrganizationsFromMSI(MSI msi) {
		return msi.organizations.stream()
				.map(o -> new Organization.Builder()
						.name(o.getName())
						.address(o.getAddress())
						.email(o.getEmail())
						.url(o.getURI())
						.role(o.getRole())
						.build()).collect(Collectors.toCollection(TreeSet::new));

	}

	private SortedSet<Contact> getContactsFromMSI(MSI msi) {
		return msi.persons.stream()
				.map(p -> new Contact.Builder()
						.firstName(p.getFirstName())
						.midInitials(p.getInitials())
						.lastName(p.getLastName())
						.email(p.getEmail())
						.role(p.getRole())
						.build())
				.collect(Collectors.toCollection(TreeSet::new));
	}



    private SortedSet<Publication> getPublicationsFromMSI(MSI msi) {
        return msi.publications.stream()
                .map(pub -> new Publication.Builder()
                .doi(pub.getDOI())
                .pubmed_id(pub.getPubMedID()).build()).collect(Collectors.toCollection(TreeSet::new));
	}

	/**
	 * Works by side effect!
	 * 
	 * Takes the sampleData and looks up existing sample within that domain   
	 *
	 */
	private void populateExistingAccessions(SampleData sampleData, String domain, Instant release, Instant update) throws DuplicateDomainSampleException {
		
		for (SampleNode sampleNode : sampleData.scd.getNodes(SampleNode.class)) {			
			//only build a sample if there is at least one attribute or it has no "parent" node
			//otherwise, it is just a group membership tracking dummy		
			if (!isDummy(sampleNode)) {
				if (sampleNode.getSampleAccession() == null) {
					//if there was no accession provided, try to find an existing accession by name and domain
					List<Filter> filterList = new ArrayList<>(2);
					filterList.add(FilterBuilder.create().onName(sampleNode.getNodeName()).build());
					filterList.add(FilterBuilder.create().onDomain(domain).build());
					Iterator<Resource<Sample>> it = bioSamplesClient.fetchSampleResourceAll(null, filterList).iterator();
	
					Resource<Sample> first = null;
					if (it.hasNext()) {
						first = it.next();
						if (it.hasNext()) {
							//error multiple accessions
							throw new DuplicateDomainSampleException(domain, sampleNode.getNodeName());
						} else {
							sampleNode.setSampleAccession(first.getContent().getAccession());
						}
					}
				}
			}			
		}
		
		for (GroupNode groupNode : sampleData.scd.getNodes(GroupNode.class)) {
			
			Sample sample = groupNodeToSample(groupNode, sampleData.msi, domain, release, update);
			if (groupNode.getGroupAccession() == null) {

				//if there was no accession provided, try to find an existing accession by name and domain
				List<Filter> filterList = new ArrayList<>(2);
				filterList.add(FilterBuilder.create().onName(sample.getName()).build());
				filterList.add(FilterBuilder.create().onDomain(sample.getDomain()).build());
				Iterator<Resource<Sample>> it = bioSamplesClient.fetchSampleResourceAll(null, filterList).iterator();
				Resource<Sample> first = null;
				if (it.hasNext()) {
					first = it.next();
					if (it.hasNext()) {
						//error multiple accessions
						throw new DuplicateDomainSampleException(sample.getDomain(), sample.getName());
					} else {
						groupNode.setGroupAccession(first.getContent().getAccession());
					}
				}
			}
		}

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
		Collection<String> iris = Lists.newArrayList();
		if (termSourceId != null && termSourceId.trim().length() > 0) {
			iris.add(termSourceId);
		}
		return Attribute.build(type, value, iris, unit);
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
		
		public ConflictingSampleTabOwnershipException(String sampleAccession, String originalSubmission, String newSubmission) {
			super("Accession "+sampleAccession+" was previouly described in "+originalSubmission);
			this.sampleAccession = sampleAccession;
			this.originalSubmission = originalSubmission;
			this.newSubmission = newSubmission;
		}
	}
	
	public static class AssertingSampleTabOwnershipException extends Exception {
		
		private static final long serialVersionUID = -1504945560846665587L;
		public final String submissionIdentifier;
		
		public AssertingSampleTabOwnershipException(String submissionIdentifier) {
			super("Submission identifier "+submissionIdentifier+" has not been previously submitted");
			this.submissionIdentifier = submissionIdentifier;
		}
	}
}
