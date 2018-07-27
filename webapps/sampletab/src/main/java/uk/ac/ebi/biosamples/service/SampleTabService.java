package uk.ac.ebi.biosamples.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Service;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.AbstractNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.Node;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.MSI;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.GroupNode;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.SampleNode;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.*;
import uk.ac.ebi.arrayexpress2.sampletab.renderer.SampleTabWriter;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.exceptions.*;
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
			throws SampleTabException {
		
		log.trace("Accessioning sampletab "+sampleData.msi.submissionIdentifier);

		//release in 100 years time
		Instant release = Instant.ofEpochSecond(LocalDateTime.now(ZoneOffset.UTC).plusYears(100).toEpochSecond(ZoneOffset.UTC));
		Instant update = Instant.ofEpochMilli(sampleData.msi.submissionUpdateDate.getTime());

		//put any existing accessions into the samplenode and groupnode objects
		populateExistingAccessions(sampleData, domain, release, update);		

		//because we are only accessioning samples and groups, we don't care about submission ownership


		// Remove all the SampleTab metadata and keep only a placeholder description
//		try {
//			sampleData = convertToPlaceholderSampletab(sampleData, release, update);
//		} catch (ParseException e) {
//			throw new SampletabAccessioningException("An error occurred producing the pre-accessioned sample placeholders");
//		}


		//persist the samples and groups
		//TODO only persist unaccessioned?
		persistSamplesAndGroups(sampleData, domain, release, update, setUpdateDate);
		
		//because we are only accessioning samples and groups, do not persist sampletab itself
		
		return sampleData;
	}


	public SampleData saveSampleTab(SampleData sampleData, String domain, boolean superUser, boolean setUpdateDate, boolean setFullDetails)
			throws SampleTabException {
		
		log.info("Saving sampletab "+sampleData.msi.submissionIdentifier);

		rejectSampletabForInvalidRelationship(sampleData);

		Instant release = Instant.ofEpochMilli(sampleData.msi.submissionReleaseDate.getTime());
		Instant update = Instant.ofEpochMilli(sampleData.msi.submissionUpdateDate.getTime());

		//verifies that accessions provided in the sampletab are owned by the APIkey and its associated domain
		verifySampletabAccessionsOwnership(sampleData, domain, release, update);

		//put any existing accessions into the samplenode and groupnode objects
        //Not that the ownership of the accession is performed also during this step
		populateExistingAccessions(sampleData, domain, release, update);


		MongoSampleTab oldSampleTab = null;
        //replace implicit derived from with explicit derived from relationships
        for (SampleNode sample : sampleData.scd.getNodes(SampleNode.class)) {

            // Check relationships with parent node
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

		// At this point all samples has been checked for an accession

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
					verifySampletabAccessionsUniqueOwnership(getSampletabSubmissionId(sampleData), accession, superUser);
				}

			} else {
				Set<String> oldAccessions = new HashSet<>(oldSampleTab.getAccessions());

				// Extract all the accessions that will be made private
				oldAccessions.removeAll(newAccessions);
				
				//check samples are owned by this sampletab and not any others
				for (String accession : newAccessions) {
					verifySampletabAccessionsUniqueOwnership(getSampletabSubmissionId(sampleData), accession, superUser);
				}
			
				//make any samples/groups that were in the old version but not the latest one private
				makeSamplesPrivate(oldAccessions, domain, update);
			}
		} else {

			String referenceAccession = null;

			if (newAccessions.size() > 0) {
				// At least one accession was provided, try to find a Submission with that accession included

                for (String accession: newAccessions) {

					List<MongoSampleTab> submissionCandidates = mongoSampleTabRepository.findByAccessionsContaining(accession);
					if (submissionCandidates.size() == 1) {
						oldSampleTab = submissionCandidates.get(0);
						referenceAccession = oldSampleTab.getId();

						Set<String> oldAccessions = new HashSet<>(oldSampleTab.getAccessions());
						// Extract all the accessions that will be made private
						oldAccessions.removeAll(newAccessions);

						//check samples are owned by this sampletab and not any others
						for (String acc : newAccessions) {
							verifySampletabAccessionsUniqueOwnership(referenceAccession, acc, superUser);
						}

						//delete any samples/groups that were in the old version but not the latest one
						makeSamplesPrivate(oldAccessions, domain, update);

						// The founded sampletab is correct
						break;


					} else if (submissionCandidates.size() > 1) {
						List<String> submissionCandidatesIdentifiers = submissionCandidates.stream().map(MongoSampleTab::getId).collect(Collectors.toList());
						throw new UnknownAccessionOwnershipException(submissionCandidatesIdentifiers, accession);
					}
				}
			}

			if (referenceAccession != null) {
				// A submission has been found from the provided accession
				sampleData.msi.submissionIdentifier = referenceAccession;
			} else {

				// A submission identifier need to be produced for the sampletab
				//no previous sampletab submission id
				//persist latest SampleTab so it gets a submission id
				//TODO: Check if we actually need to submit a sampletab without accession or not
				sampleData.msi.submissionIdentifier = generateSubmissionIdentifier(sampleData, domain);

				//check samples are not owned by any others
				for (String accession : newAccessions) {
					verifySampletabAccessionsUniqueOwnership(getSampletabSubmissionId(sampleData), accession, superUser);
				}

			}

		}
		
		//at this point, sampleData must have a sane submissionIdentifier
		if (sampleData.msi.submissionIdentifier == null || sampleData.msi.submissionIdentifier.trim().length() == 0) {
			throw new RuntimeException("Failed to find submission identifier");
		}

        if (sampleData.scd.getNodes(GroupNode.class).size() == 0) {
        	// TODO need to check submissionIdentifier is populated correctly

			// Check if all samples are already part of a group

			groupSamplesInSampletab(sampleData, "Submission "+ getSampletabSubmissionId(sampleData), null);
        }


		//persist the samples and groups
		persistSamplesAndGroups(sampleData, domain, release, update, setUpdateDate);

		//persist updated SampleTab so has all the associated accessions added
		persistSampleTab(sampleData, domain);
		
		return sampleData;
	}




	/**
	 * Persist a sampletab data, accessions and file to MongoCollection generating an ID
	 *
	 * @param sampleData the sampletab data
	 * @param domain the domain to use for the submission
	 */
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

		String sampleTab = convertToSampletabDoc(sampleData);
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


	private String convertToSampletabDoc(SampleData sampleData) {

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

		return sampleTab;

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
		//now that everything has an accession, need to update relationships that were by name
		for (String futureName : futureMap.keySet()) {
			Sample sample;
			try {
				sample = futureMap.get(futureName).get().getContent();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
			boolean changed = false;
			Collection<Relationship> addRelationships = new HashSet<>();
			Collection<Relationship> removeRelationships = new HashSet<>();
			for (Relationship relationship : sample.getRelationships()) {
				if (relationship.getSource().equals(sample.getAccession())) {
					if (!relationship.getTarget().matches("SAM[END][AG]?[0-9]+")) {
						//this is a relationship that points to a non accessions
						//check if any of the samples here  have a name that matches
						//if so, update the relationship to use the accession
						if (futureMap.keySet().contains(relationship.getTarget())) {							
							Sample target;
							try {
								target = futureMap.get(relationship.getTarget()).get().getContent();
							} catch (InterruptedException | ExecutionException e) {
								throw new RuntimeException(e);
							}							
							removeRelationships.add(relationship);
							addRelationships.add(Relationship.build(sample.getAccession(), 
									relationship.getType(), target.getAccession()));
							changed = true;

							// Update also sampletab node entry
							SampleNode sampleNode = sampleData.scd.getNode(futureName, SampleNode.class);
							SCDNodeAttribute newRelationship = null;

							// Create the relationship node based on the relationship type
							switch (relationship.getType()) {
								case "derived from":
									newRelationship = new DerivedFromAttribute(target.getAccession());
									break;
								case "same as":
									newRelationship = new SameAsAttribute(target.getAccession());
									break;
								case "child of":
									newRelationship = new ChildOfAttribute(target.getAccession());
									break;
							}

							// Search for the corresponding attribute with
							List<SCDNodeAttribute> nodeAttrList = sampleNode.getAttributes().stream()
									.filter(attr -> attr.getAttributeType().equalsIgnoreCase(relationship.getType()))
									.filter(attr -> attr.getAttributeValue().equals(relationship.getTarget()))
									.collect(Collectors.toList());
							if (nodeAttrList.size() == 1) {
								SCDNodeAttribute nodeAttr = nodeAttrList.get(0);
								sampleNode.removeAttribute(nodeAttr);
								sampleNode.addAttribute(newRelationship);
							} else {
								// This should not happen at this stage of the process
								throw new RuntimeException("Sample " + futureName + " has a relationship with " +
										relationship.getTarget() + " which is not part of the same sampletab");
							}
						} else {
							// This should not happen at this stage of the process
							throw new RuntimeException("Sample " + futureName + " has a relationship with " +
									relationship.getTarget() + " which is not part of the same sampletab");
						}
					}
				}
			}
			if (changed) {
				sample.getRelationships().removeAll(removeRelationships);
				sample.getRelationships().addAll(addRelationships);
				futureMap.put(sample.getName(), bioSamplesClient.persistSampleResourceAsync(sample, setUpdateDate, true));
			}
		}
		//check any updated futures to make sure they are finished
		for (String futureName : futureMap.keySet()) {
			Sample sample;
			try {
				sample = futureMap.get(futureName).get().getContent();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
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
//			sample = Sample.build(sample.getName(), sample.getAccession(), sample.getDomain(),
//					sample.getRelease(), sample.getUpdate(), sample.getAttributes(), relationships, sample.getExternalReferences(),
//					sample.getOrganizations(), sample.getContacts(), sample.getPublications());
            sample = Sample.Builder.fromSample(sample).withRelationships(relationships).build();

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
//		Sample sample = Sample.build(name, accession, domain, release, update,
//				attributes, relationships, externalReferences,
//				organizations, contacts, publications);
        Sample sample = new Sample.Builder(name, accession).withDomain(domain)
				.withRelease(release).withUpdate(update)
				.withAttributes(attributes).withRelationships(relationships).withExternalReferences(externalReferences)
				.withOrganizations(organizations).withContacts(contacts).withPublications(publications)
				.build();

		return sample;
	}
	
	private Sample sampleNodeToSample(SampleNode sampleNode, MSI msi, String domain, Instant release, Instant update) {

		String accession = sampleNode.getSampleAccession();
		String name = sampleNode.getNodeName();

		SortedSet<Attribute> attributes = new TreeSet<>();
		SortedSet<Relationship> relationships = new TreeSet<>();
//		SortedSet<ExternalReference> externalReferences = getExternalReferencesFromSampleNode(sampleNode);
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
		//TODO: Whydo we need to have a submission identifier if for the accessioning service we don't even store the sampletab?
		if (msi.submissionIdentifier != null && msi.submissionIdentifier.trim().length() > 0) {
			attributes.add(Attribute.build("Submission identifier", msi.submissionIdentifier));
		}

		if (msi.submissionDescription != null && msi.submissionDescription.trim().length() > 0) {
			attributes.add(Attribute.build("Submission description", msi.submissionDescription));			
		}

		if (msi.submissionTitle != null && msi.submissionTitle.trim().length() > 0) {
			attributes.add(Attribute.build("Submission title", msi.submissionTitle));			
		}


//		Sample sample = Sample.build(name, accession, domain, release, update,
//				attributes, relationships, externalReferences,
//				organizations, contacts, publications);
//		return sample;
        return new Sample.Builder(name, accession).withDomain(domain)
				.withRelease(release).withUpdate(update)
				.withAttributes(attributes).withRelationships(relationships).withExternalReferences(externalReferences)
				.withOrganizations(organizations).withContacts(contacts).withPublications(publications)
				.build();
	}

	private SortedSet<ExternalReference> getExternalReferencesFromSampleNode(SampleNode sampleNode) {
	    return sampleNode.getAttributes().stream()
				.filter(attr -> attr.getAttributeType().equalsIgnoreCase("Database URI"))
				.map(extRef-> ExternalReference.build(extRef.getAttributeValue()))
				.collect(Collectors.toCollection(TreeSet::new));
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

		String referenceGroupAccession = null;

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
							Sample sample = first.getContent();
							sampleNode.setSampleAccession(sample.getAccession());

							// Get sample group relationships
							List<Relationship> hasMemberRelationships = first.getContent().getRelationships().stream()
									.filter(r -> r.getType().equalsIgnoreCase("has member") &&
											r.getTarget().equals(sample.getAccession())).collect(Collectors.toList());
							if (hasMemberRelationships.size() > 1) {
								throw new RuntimeException(String.format("Sample %s is part of multiple groups: %s",
										sample.getAccession(),
										String.join(", ", hasMemberRelationships.stream()
										.map(Relationship::getTarget)
										.collect(Collectors.toList()))));
							} else if (hasMemberRelationships.size() == 1) {
								String groupAccessionCandidate = hasMemberRelationships.get(0).getSource();

                                if (referenceGroupAccession != null && !referenceGroupAccession.equals(groupAccessionCandidate)) {
                                    throw new RuntimeException("Samples in provided sampletab members of " +
                                            "different groups: "+referenceGroupAccession + ", " + groupAccessionCandidate);
                                } else {
									referenceGroupAccession = groupAccessionCandidate;
								}
							}

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

		if (referenceGroupAccession != null) {
			Optional<Resource<Sample>> optionalGroup = bioSamplesClient.fetchSampleResource(referenceGroupAccession);
			if (!optionalGroup.isPresent()) {
				throw new RuntimeException("Sampletab referencing non existing group " + referenceGroupAccession);
			} else {
				Sample group = optionalGroup.get().getContent();
				groupSamplesInSampletab(sampleData, group.getName(), group.getAccession());
			}

		}

	}

	private void groupSamplesInSampletab(SampleData sampleData, String name, String accession) {
		GroupNode otherGroup = new GroupNode(name);

		if (accession != null) {
			otherGroup.setGroupAccession(accession);
		}

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

			if (!sampleInGroup) {
				log.info("Adding sample " + sample.getNodeName() + " to group " + otherGroup.getNodeName());
				otherGroup.addSample(sample);
			}
		}
		//only add the new group if it has any samples
		if (otherGroup.getParentNodes().size() > 0) {
			try {
				sampleData.scd.addNode(otherGroup);
			} catch (ParseException e) {
				//this should never happen
				throw new RuntimeException(e);
			}
			log.info("Added group node \"" + otherGroup.getNodeName() + "\"");
			// also need to accession the new node
		}
	}

	/**
	 * Verify accessions inside of the SampleData object are owned by the domain used to save the sampletab
	 * @param sampleData the parsed sampletab data
	 * @param domain domain that should own the sampletab and the accessions within it
	 * @param release instant used to create the group associated to the sampletab
	 * @param update instant used to create the group associated to the sampletab
	 * @throws DomainOwnershipException
	 */
	private void verifySampletabAccessionsOwnership(SampleData sampleData, String domain, Instant release, Instant update) throws DomainOwnershipException {

		for (SampleNode sampleNode : sampleData.scd.getNodes(SampleNode.class)) {
			//only build a sample if there is at least one attribute or it has no "parent" node
			//otherwise, it is just a group membership tracking dummy
			if (!isDummy(sampleNode)) {
				if (sampleNode.getSampleAccession() != null) {
					List<Filter> filterList = new ArrayList<>(2);
					filterList.add(FilterBuilder.create().onDomain(domain).build());
					filterList.add(FilterBuilder.create().onAccession(sampleNode.getSampleAccession()).build());
					Iterator<Resource<Sample>> it = bioSamplesClient.fetchSampleResourceAll(null, filterList).iterator();
					if (it.hasNext()) {
						it.next();
						if (it.hasNext()) {
							// Error because multiple samples with the same accesion has been found and it should never happen
							throw new RuntimeException("More than one sample found with the same accession " + sampleNode.getSampleAccession());
						}
					} else {
						throw new DomainOwnershipException(domain, sampleNode.getSampleAccession());
					}

				}
			}
		}

		for (GroupNode groupNode : sampleData.scd.getNodes(GroupNode.class)) {

			Sample sample = groupNodeToSample(groupNode, sampleData.msi, domain, release, update);
			if (groupNode.getGroupAccession() != null) {

				//if there was no accession provided, try to find an existing accession by name and domain
				List<Filter> filterList = new ArrayList<>(2);
				filterList.add(FilterBuilder.create().onAccession(sample.getAccession()).build());
				filterList.add(FilterBuilder.create().onDomain(sample.getDomain()).build());
				Iterator<Resource<Sample>> it = bioSamplesClient.fetchSampleResourceAll(null, filterList).iterator();
				if (it.hasNext()) {
					it.next();
					if (it.hasNext()) {
						//error multiple accessions
						throw new RuntimeException("More than one group found with accession "+groupNode.getGroupAccession()+ " and domain "+domain);
					}
				} else {
					throw new DomainOwnershipException(domain, groupNode.getGroupAccession());
				}
			}
		}

	}


	/**
	 * Verify accession is part of a single sampletab and is not owned by any other submission
	 *
	 * This method verifies that given a sampletab and an accession, there is no other submission that
	 * @param sampletabSubmissionIdentifier the submission ID for the sampletab
	 * @param accession the accession to check
	 * @throws ConflictingSampleTabOwnershipException
	 */
	private void verifySampletabAccessionsUniqueOwnership(String sampletabSubmissionIdentifier, String accession, Boolean isSuperUser) throws ConflictingSampleTabOwnershipException, SampleTabWithUnacceptableAccessionException {

		List<MongoSampleTab> sampletabsContainingAccession = mongoSampleTabRepository.findByAccessionsContaining(accession);

		if (sampletabsContainingAccession == null) {
			log.info("Null accession sample tabs for accession "+accession);
		} else if (sampletabsContainingAccession.size() == 0) {
			log.info("No accession sample tabs for accession "+accession);
			if (!isSuperUser) {
				throw new SampleTabWithUnacceptableAccessionException(accession);
			}
		} else if (sampletabsContainingAccession.size() > 1) {
			log.warn("Multiple accession sample tabs for accession "+accession);
			MongoSampleTab accessionSampleTab = sampletabsContainingAccession.get(0);
			String existingSubmissionIdentifier = accessionSampleTab.getId().trim();
			throw new ConflictingSampleTabOwnershipException(accession, existingSubmissionIdentifier, sampletabSubmissionIdentifier);
		} else {

			log.info("One accession sample tabs for accession "+accession);
			MongoSampleTab accessionSampleTab = sampletabsContainingAccession.get(0);
			String existingSubmissionIdentifier = accessionSampleTab.getId().trim();
			log.info("existingId = "+existingSubmissionIdentifier);
			log.info("newId = "+sampletabSubmissionIdentifier);
			if (!existingSubmissionIdentifier.equals(sampletabSubmissionIdentifier)) {
				//this sample is "owned" by a different sampletab file
				throw new ConflictingSampleTabOwnershipException(accession,
						existingSubmissionIdentifier, sampletabSubmissionIdentifier);
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

	/**
	 * Given the sampletab data, removes the MSI information as well as all the characteristics and relationships
	 * to make the samples and groups placeholders
	 * @param sampleData the sampletab data to process
     * @param updateDate the update date to use for the submission
	 * @param releaseDate the release date to use for the submission
	 * @return a new sampletab content cleaned from everything except samples and groups
	 */
	private SampleData convertToPlaceholderSampletab(SampleData sampleData, Instant releaseDate, Instant updateDate) throws ParseException {

		SampleData newSampledata = new SampleData();

		// Create samples placeholders
		for (SampleNode sampleNode: sampleData.scd.getNodes(SampleNode.class)) {
		    SampleNode sampleNodePlaceholder = new SampleNode();
		    sampleNodePlaceholder.setNodeName(sampleNode.getNodeName());
		    sampleNodePlaceholder.setSampleAccession(sampleNode.getSampleAccession());
			sampleNodePlaceholder.setSampleDescription("This is a placeholder for a pre-accessioned sample");

		    newSampledata.scd.addNode(sampleNodePlaceholder);
		}

		for (GroupNode groupNode: sampleData.scd.getNodes(GroupNode.class)) {
			groupNode.setGroupDescription("This is a pre-accessioned sample placeholder");
			newSampledata.scd.addNode(groupNode);
		}

		// Update MSI infos
		newSampledata.msi.submissionIdentifier = "";
		newSampledata.msi.submissionDescription = "This sampletab contains accessions for future submissions";
		newSampledata.msi.submissionTitle = "Accessioned sampletab";
		newSampledata.msi.submissionReleaseDate.setTime(releaseDate.toEpochMilli());
		newSampledata.msi.submissionUpdateDate.setTime(updateDate.toEpochMilli());

		return newSampledata;
	}



	private Attribute makeAttribute(String type, String value, String termSourceId, String unit) {
		Collection<String> iris = Lists.newArrayList();
		if (termSourceId != null && termSourceId.trim().length() > 0) {
			iris.add(termSourceId);
		}
		return Attribute.build(type, value, iris, unit);
	}

	private void rejectSampletabForInvalidRelationship(SampleData sampleData) throws UnexpectedSampleTabRelationshipException {

		Collection<SampleNode> sampleNodes = sampleData.scd.getNodes(SampleNode.class);
		List<String> sampleNames = sampleNodes.stream().map(AbstractNode::getNodeName).collect(Collectors.toList());
		for(SampleNode sampleNode: sampleNodes) {
			Optional<AbstractRelationshipAttribute> invalidRelationship = sampleNode.getAttributes().stream()
					.filter(AbstractRelationshipAttribute.class::isInstance)
					.map(node -> (AbstractRelationshipAttribute) node)
					.filter(node -> !(node.getAttributeValue().matches("SAM[END][AG]?[0-9]+") || sampleNames.contains(node.getAttributeValue())))
					.findAny();

			if (invalidRelationship.isPresent()) {
				throw new UnexpectedSampleTabRelationshipException(sampleNode.getNodeName(),
						invalidRelationship.get().getAttributeType(),
						invalidRelationship.get().getAttributeValue());
			}

		}
	}

	/**
	 * Make a set of samples private
	 *
	 * @param sampleAccessions the accession of the samples to make private
	 * @param domain the domain to use for the update
	 * @param update the update date
	 */
	private void makeSamplesPrivate(Set<String> sampleAccessions, String domain, Instant update) {
		for (String sampleToMakePrivate : sampleAccessions) {
			//get the existing version to be "deleted"
			Optional<Resource<Sample>> oldSample = bioSamplesClient.fetchSampleResource(sampleToMakePrivate);
			if (oldSample.isPresent()) {
				//don't do a hard-delete, instead mark it as public in 100 years
				Sample sample = Sample.build(oldSample.get().getContent().getName(), sampleToMakePrivate, domain,
						ZonedDateTime.now(ZoneOffset.UTC).plusYears(100).toInstant(), update,
						new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
				bioSamplesClient.persistSampleResource(sample, true,true ).getContent();
			}
		}
	}

	/**
	 * Generate a submission identifier for the provided sampletab
	 * @param sampleData the Sampletab data
	 * @param domain the domain to associate
	 * @throws RuntimeException if trying to generate a new submission id for a sampletab with already a submission identifier
	 * @return
	 */
	private String generateSubmissionIdentifier(SampleData sampleData, String domain) {

		String submissionId = getSampletabSubmissionId(sampleData);

		if (submissionId != null && !submissionId.isEmpty()) {
			throw new RuntimeException("Trying to generate a submission identifier for sampletab already having submission identifier");
		}

		String sampleTab = convertToSampletabDoc(sampleData);
		//actually persist it
		//this will assign a new submission identifier if needed
		MongoSampleTab mongoSampleTab = MongoSampleTab.build(sampleData.msi.submissionIdentifier, domain, sampleTab, Collections.emptyList());
		log.info("Generating new sampletab identifier");
		mongoSampleTab = sampleTabIdSerivce.accessionAndInsert(mongoSampleTab);
		log.info("Generated new sampletab identifier "+mongoSampleTab.getId());

		return mongoSampleTab.getId();


	}

	private String getSampletabSubmissionId(SampleData sampleData) {

	    if (sampleData.msi.submissionIdentifier != null)
				return sampleData.msi.submissionIdentifier.trim();
	    else
				return sampleData.msi.submissionIdentifier;
	}
}
