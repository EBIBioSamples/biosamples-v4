package uk.ac.ebi.biosamples.migration;

import com.google.common.collect.Sets;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import uk.ac.ebi.biosamples.legacy.json.service.JSONSampleToSampleConverter;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.service.XmlGroupToSampleConverter;
import uk.ac.ebi.biosamples.service.XmlSampleToSampleConverter;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


class LegacyJsonAccessionComparisonCallable implements Callable<Void> {
	private final RestTemplate restTemplate;
	private final String oldUrl;
	private final String newUrl;
	private final Queue<String> bothQueue;
	private final AtomicBoolean bothFlag;
	private final JSONSampleToSampleConverter legacyJsonConverter;
	private final boolean compare;

	private final Logger log = LoggerFactory.getLogger(getClass());

	public LegacyJsonAccessionComparisonCallable(RestTemplate restTemplate, String oldUrl, String newUrl, Queue<String> bothQueue,
                                                 AtomicBoolean bothFlag,
                                                 JSONSampleToSampleConverter legacyJsonConverter,
                                                 boolean compare) {
		this.restTemplate = restTemplate;
		this.oldUrl = oldUrl;
		this.newUrl = newUrl;
		this.bothQueue = bothQueue;
		this.bothFlag = bothFlag;
		this.legacyJsonConverter = legacyJsonConverter;
		this.compare = compare;
	}

	@Override
	public Void call() throws Exception {
		log.info("Started");
		log.info("oldUrl = "+oldUrl);
		log.info("newUrl = "+newUrl);
		log.info("compare = "+compare);
		SortedSet<String> problemAccessions = new TreeSet<>();

		while (!bothFlag.get() || !bothQueue.isEmpty()) {
			String accession = bothQueue.poll();
			if (accession != null) {
				log.info("Comparing accession "+ accession);
				if (compare) {
					try {
						compare(accession);
					} catch (RestClientException e) {
						//there was a rest error, log it and continue
						problemAccessions.add(accession);
						log.error("Problem accessing "+accession, e);
					}
				}
			} else {
				Thread.sleep(100);
//				log.info("Waiting while accession is null");
			}
		}
		for (String accession : problemAccessions) {
			log.error("Problem accessing "+accession);
		}
		log.info("Finished AccessionComparisonCallable.call(");
		return null;
	}

	public void compare(String accession) {
		log.info("Comparing accession " + accession);

		UriComponentsBuilder oldUriComponentBuilder = UriComponentsBuilder.fromUriString(oldUrl);
		UriComponentsBuilder newUriComponentBuilder = UriComponentsBuilder.fromUriString(newUrl);

		String endpoint = accession.startsWith("SAMEG") ? "groups" : "samples";
		//ComparisonFormatter comparisonFormatter = new DefaultComparisonFormatter();
        URI oldUri = oldUriComponentBuilder.cloneBuilder().pathSegment(endpoint, accession).build().toUri();
        URI newUri = newUriComponentBuilder.cloneBuilder().pathSegment(endpoint, accession).build().toUri();
        String oldDocument = getDocument(oldUri);
        String newDocument = getDocument(newUri);

        Sample newSample = legacyJsonConverter.convert(newDocument);
        Sample oldSample = legacyJsonConverter.convert(oldDocument);


		if (!oldSample.getAccession().equals(newSample.getAccession())) {
			log.warn("Difference on "+accession+" of accession between '"+oldSample.getAccession()+"' and '"+newSample.getAccession()+"'");
		}
		if (!oldSample.getName().equals(newSample.getName())) {
			log.warn("Difference on "+accession+" of name between '"+oldSample.getName()+"' and '"+newSample.getName()+"'");
		}
				
		if (Math.abs(ChronoUnit.DAYS.between(oldSample.getUpdate(), newSample.getUpdate())) > 1) {
			log.warn("Difference on "+accession+" of update date between '"+oldSample.getUpdate()+"' and '"+newSample.getUpdate()+"'");
		}
		if (Math.abs(ChronoUnit.DAYS.between(oldSample.getRelease(), newSample.getRelease())) > 1) {
			log.warn("Difference on "+accession+" of release date between '"+oldSample.getRelease()+"' and '"+newSample.getRelease()+"'");
		}
		
		compareAttributes(oldSample, newSample);
		
		//relationships
		for (Relationship relationship : Sets.difference(oldSample.getRelationships(), newSample.getRelationships())) {
			log.warn("Difference on "+accession+" of relationship '"+relationship+"' in old only");			
		}
		for (Relationship relationship : Sets.difference(newSample.getRelationships(), oldSample.getRelationships())) {
			log.warn("Difference on "+accession+" of relationship '"+relationship+"' in new only");			
		}
		
		//if it is a group, get the samples within each environment, and compare them
		if (accession.startsWith("SAMEG")) {
			SortedSet<Relationship> oldGroupMembers = oldSample.getRelationships();
			SortedSet<Relationship> newGroupMembers = newSample.getRelationships();
		}

		compareExternalReferences(oldSample, newSample);

		compareOrganizations(oldSample, newSample);

		compareContacts(oldSample, newSample);

		comparePublications(oldSample, newSample);
	}



	public String getDocument(URI uri) {
		//log.info("Getting " + uri);
		ResponseEntity<String> response;
		try {
			response = restTemplate.getForEntity(uri, String.class);
		} catch (RestClientException e) {
			log.error("Problem accessing " + uri, e);
			throw e;
		}
		return response.getBody();
	}



	private void compareAttributes(Sample oldSample, Sample newSample) {
		String accession = oldSample.getAccession();

		Set<String> oldAttributeTypes = oldSample.getAttributes().stream().map(a -> a.getType()).collect(Collectors.toSet());
		Set<String> newAttributeTypes = newSample.getAttributes().stream().map(a -> a.getType()).collect(Collectors.toSet());

		for (String attributeType : Sets.difference(oldAttributeTypes, newAttributeTypes)) {
			log.warn("Difference on "+accession+" of attribute '"+attributeType+"' in old only");
		}

		for (String attributeType : Sets.difference(newAttributeTypes, oldAttributeTypes)) {
			log.warn("Difference on "+accession+" of attribute '"+attributeType+"' in new only");
		}

		for (String attributeType : Sets.intersection(oldAttributeTypes, newAttributeTypes)) {
			List<Attribute> oldAttributes = oldSample.getAttributes().stream()
					.filter(a -> attributeType.equals(a.getType())).collect(Collectors.toList());
			Collections.sort(oldAttributes);
			List<Attribute> newAttributes = newSample.getAttributes().stream()
					.filter(a -> attributeType.equals(a.getType())).collect(Collectors.toList());
			Collections.sort(newAttributes);

			SortedMap<String, SortedMap<String, String>> oldUnits = new TreeMap<>();
			SortedMap<String, SortedMap<String, SortedSet<String>>> oldIris = new TreeMap<>();
			SortedMap<String, SortedMap<String, String>> newUnits = new TreeMap<>();
			SortedMap<String, SortedMap<String, SortedSet<String>>> newIris = new TreeMap<>();

			if (oldAttributes.size() != newAttributes.size()) {
				log.warn("Difference on " + accession + " of attribute '" + attributeType + "' has " + oldAttributes.size() + " values in old and " + newAttributes.size() + " values in new");
			} else {
				for (int i = 0; i < oldAttributes.size(); i++) {
					Attribute oldAttribute = oldAttributes.get(i);
					Attribute newAttribute = newAttributes.get(i);

					//TODO finish me

					if (!oldUnits.containsKey(oldAttribute.getType())) {
						oldUnits.put(oldAttribute.getType(), new TreeMap<>());
					}
					if (!oldIris.containsKey(oldAttribute.getType())) {
						oldIris.put(oldAttribute.getType(), new TreeMap<>());
					}

					if (oldAttribute.getValue() != null) {
						if (oldAttribute.getUnit() != null) {
							oldUnits.get(oldAttribute.getType()).put(oldAttribute.getValue(), oldAttribute.getUnit());
						}
						oldIris.get(oldAttribute.getType()).put(oldAttribute.getValue(), oldAttribute.getIri());
					}

					if (!newUnits.containsKey(newAttribute.getType())) {
						newUnits.put(newAttribute.getType(), new TreeMap<>());
					}

					if (!newIris.containsKey(newAttribute.getType())) {
						newIris.put(newAttribute.getType(), new TreeMap<>());
					}

					if (newAttribute.getValue() != null) {
					    if (newAttribute.getUnit() != null) {
							newUnits.get(newAttribute.getType()).put(newAttribute.getValue(), newAttribute.getUnit());
						}
						newIris.get(newAttribute.getType()).put(newAttribute.getValue(), newAttribute.getIri());
					}

					//compare values
					if (!Objects.equals(oldAttribute.getValue(),newAttribute.getValue())) {
						log.warn("Difference on " + accession + " of attribute '" + attributeType + "' between '" + oldAttribute.getValue() + "' and '" + newAttribute.getValue() + "'");
					}

					//compare units
					if (oldAttribute.getUnit() != null && newAttribute.getUnit() != null
							&& !oldAttribute.getUnit().equals(newAttribute.getUnit())) {
						log.warn("Difference on " + accession + " of attribute '" + attributeType + "' between units '" + oldAttribute.getUnit() + "' and '" + newAttribute.getUnit() + "'");
					}
					//compare iris
					if (!Objects.equals(oldAttribute.getIri(),newAttribute.getIri())) {
						if (oldAttribute.getIri().size() < newAttribute.getIri().size()) {
							log.warn("Difference on " + accession + " of attribute '" + attributeType + "' between iris '" + oldAttribute.getIri() + "' and '" + newAttribute.getIri() + "'");
						} else if (oldAttribute.getIri().size() > newAttribute.getIri().size()) {
							log.warn("Difference on " + accession + " of attribute '" + attributeType + "' between iris '" + oldAttribute.getIri() + "' and '" + newAttribute.getIri() + "'");
						} else {
							Iterator<String> thisIt = oldAttribute.getIri().iterator();
							Iterator<String> otherIt = newAttribute.getIri().iterator();
							while (thisIt.hasNext() && otherIt.hasNext()) {
								int val = thisIt.next().compareTo(otherIt.next());
								if (val != 0) {
									log.warn("Difference on " + accession + " of attribute '" + attributeType + "' between iris '" + oldAttribute.getIri() + "' and '" + newAttribute.getIri() + "'");
								}
							}
						}
					}
				}
			}
		}
	}

	private void compareOrganizations(Sample oldSample, Sample newSample) {
		SortedSet<Organization> oldOrganizations = oldSample.getOrganizations();
		SortedSet<Organization> newOrganizations = newSample.getOrganizations();

		for (Organization oldOrganization: Sets.difference(oldOrganizations, newOrganizations)) {
			log.warn("Difference on "+oldSample.getAccession()+" organization: only old sample has " + oldOrganization.toString());
		}

		for (Organization newOrganization: Sets.difference(newOrganizations, oldOrganizations)) {
			log.warn("Difference on "+oldSample.getAccession()+" organization: only new sample has " + newOrganization.toString());
		}

	}

	private void compareContacts(Sample oldSample, Sample newSample) {
		SortedSet<Contact> oldContacts = oldSample.getContacts();
		SortedSet<Contact> newContacts = newSample.getContacts();

		for (Contact oldContact: Sets.difference(oldContacts, newContacts)) {
			log.warn("Difference on "+oldSample.getAccession()+" contact: only old sample has " + oldContact.toString());
		}

		for (Contact newContact: Sets.difference(newContacts, oldContacts)) {
			log.warn("Difference on "+oldSample.getAccession()+" contact: only new sample has " + newContact.toString());
		}
	}

	private void comparePublications(Sample oldSample, Sample newSample) {
		SortedSet<Publication> oldPublications = oldSample.getPublications();
		SortedSet<Publication> newPublications = newSample.getPublications();

		for(Publication oldPublication: Sets.difference(oldPublications, newPublications)) {
			log.warn("Difference on "+oldSample.getAccession()+" pulication: only old sample has " + oldPublication.toString());
		}

		for(Publication newPublication: Sets.difference(newPublications, oldPublications)) {
			log.warn("Difference on "+oldSample.getAccession()+" pulication: only new sample has " + newPublication.toString());
		}
	}

	private void compareExternalReferences(Sample oldSample, Sample newSample) {
		SortedSet<ExternalReference> oldExternalReferences = oldSample.getExternalReferences();
		SortedSet<ExternalReference> newExternalReferences = newSample.getExternalReferences();

		for (ExternalReference oldExternalReference: Sets.difference(oldExternalReferences, newExternalReferences)) {
			log.warn("Difference on "+oldSample.getAccession()+" external reference: only old sample has " + oldExternalReference.toString());
		}

		for (ExternalReference newExternalReference: Sets.difference(newExternalReferences, oldExternalReferences)) {
			log.warn("Difference on "+oldSample.getAccession()+" external reference: only new sample has " + newExternalReference.toString());
		}
	}
}