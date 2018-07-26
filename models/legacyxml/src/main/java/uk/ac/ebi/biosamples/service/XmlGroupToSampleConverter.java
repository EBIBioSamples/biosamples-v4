package uk.ac.ebi.biosamples.service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Contact;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Organization;
import uk.ac.ebi.biosamples.model.Publication;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

@Service
public class XmlGroupToSampleConverter implements Converter<Element, Sample>  {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Override
	public Sample convert(Element root) {
		
		Instant release = Instant.now();
		Instant update = Instant.now();
		String accession = null;
		if (XmlPathBuilder.of(root).attributeExists("id")) {
			accession = XmlPathBuilder.of(root).attribute("id");
		}
		String name = null;
		SortedSet<Attribute> attributes = new TreeSet<>();
		SortedSet<Relationship> relationships = new TreeSet<>();
		SortedSet<ExternalReference> externalReferences = new TreeSet<>();
		
		for (Element property : XmlPathBuilder.of(root).elements("Property")){
			if ("Group Name".equals(XmlPathBuilder.of(property).attribute("class"))) {
				name = XmlPathBuilder.of(property).path("QualifiedValue", "Value").text();
			} else if ("Group Description".equals(XmlPathBuilder.of(property).attribute("class"))) {
				String value = XmlPathBuilder.of(property).path("QualifiedValue", "Value").text();
				attributes.add(Attribute.build("description", value));
			} else if ("Submission Release Date".equals(XmlPathBuilder.of(property).attribute("class"))) {
				release = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(XmlPathBuilder.of(property).path("QualifiedValue", "Value").text()));
			} else if ("Submission Update Date".equals(XmlPathBuilder.of(property).attribute("class"))) {
				update = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(XmlPathBuilder.of(property).path("QualifiedValue", "Value").text()));
			} else {
				String type = XmlPathBuilder.of(property).attribute("class");
				
				for (Element qualifiedValue : XmlPathBuilder.of(property).elements("QualifiedValue")) {
				
					String value = XmlPathBuilder.of(qualifiedValue).path("Value").text();
					Collection<String> iri = Lists.newArrayList();
					String unit = null;
					
					if (XmlPathBuilder.of(qualifiedValue).path("TermSourceREF").exists()
							&& XmlPathBuilder.of(qualifiedValue).path("TermSourceREF", "TermSourceID").exists()) {
						iri.add(XmlPathBuilder.of(qualifiedValue).path("TermSourceREF", "TermSourceID").text());
					}
					
					if (XmlPathBuilder.of(qualifiedValue).path("TermSourceREF").exists()
							&& XmlPathBuilder.of(qualifiedValue).path("TermSourceREF", "Unit").exists()) {
						unit = XmlPathBuilder.of(qualifiedValue).path("TermSourceREF", "Unit").text();
					}
					
					attributes.add(Attribute.build(type, value, iri, unit));		
				}
			}
		}

		SortedSet<Contact> contacts = extractContacts(root);
		SortedSet<Publication> publications = extractPublications(root);
		SortedSet<Organization> organizations = extractOrganizations(root);

		for (Element database : XmlPathBuilder.of(root).elements("Database")){
			if (XmlPathBuilder.of(database).path("URI").exists()) {
				externalReferences.add(ExternalReference.build(XmlPathBuilder.of(database).path("URI").text()));
			}
		}
		
		//relationships
		if (XmlPathBuilder.of(root).path("SampleIds").exists()) {
			for (Element id : XmlPathBuilder.of(root).path("SampleIds").elements("Id")) {
				relationships.add(Relationship.build(accession, "has member", id.getTextTrim()));
			}
		}
		
		log.trace("name = "+name);
		log.trace("accession = "+accession);
		log.trace("release = "+release);
		log.trace("update = "+update);
		log.trace("attributes = "+attributes);
		log.trace("relationships = "+relationships);
		
//		return Sample.build(name, accession, null, release, update,
//				attributes, relationships, externalReferences,
//				organizations, contacts, publications);
        return new Sample.Builder(name, accession).withDomain(null)
				.withRelease(release).withUpdate(update)
				.withAttributes(attributes).withRelationships(relationships)
				.withExternalReferences(externalReferences).withOrganizations(organizations)
				.withContacts(contacts).withPublications(publications)
				.build();
	}

	private SortedSet<Contact> extractContacts(Element doc) {
		SortedSet<Contact> contacts = new TreeSet<>();
		String[] contactFields = new String[] {
				"FirstName",
				"LastName",
				"MidInitials",
				"Email",
				"Role"
		};

		for (Element elem: XmlPathBuilder.of(doc).elements("Person")) {
			Map<String, String> fieldMap = extractFields(elem, contactFields);
			Contact contact = new Contact.Builder()
					.firstName(fieldMap.get("FirstName"))
					.lastName(fieldMap.get("LastName"))
					.midInitials(fieldMap.get("MidInitials"))
					.email(fieldMap.get("Email"))
					.role(fieldMap.get("Role"))
					.build();

			contacts.add(contact);
		}

		return contacts;
	}

	private SortedSet<Publication> extractPublications(Element doc) {
		SortedSet<Publication> publications = new TreeSet<>();
		String[] publicationFields = new String[] {
				"DOI",
				"PubMedID"
		};

		for (Element elem: XmlPathBuilder.of(doc).elements("Publication")) {
			Map<String, String> fieldMap = extractFields(elem, publicationFields);
			Publication publication = new Publication.Builder()
					.doi(fieldMap.get("DOI"))
					.pubmed_id(fieldMap.get("PubMedID"))
					.build();

			publications.add(publication);
		}

		return publications;
	}

	private SortedSet<Organization> extractOrganizations(Element doc) {
		SortedSet<Organization> organizations = new TreeSet<>();
		String[] organizationFields = new String[] {
				"Name",
				"Address",
				"URI",
				"Email",
				"Role"
		};

		for (Element elem: XmlPathBuilder.of(doc).elements("Organization")) {
			Map<String, String> fieldMap = extractFields(elem, organizationFields);
			Organization organization = new Organization.Builder()
					.name(fieldMap.get("Name"))
					.address(fieldMap.get("Address"))
					.url(fieldMap.get("URI"))
					.email(fieldMap.get("Email"))
					.role(fieldMap.get("Role"))
					.build();

			organizations.add(organization);
		}

		return organizations;
	}


	private Map<String, String> extractFields(Element element, String... fields) {
	    Map<String,String> mappedFields = new HashMap<>();

	    for (String field: fields) {

	    	XmlPathBuilder fieldBuilder = XmlPathBuilder.of(element).path(field);
	    	if (fieldBuilder.exists())
	    		mappedFields.put(field, fieldBuilder.text());
			else
				mappedFields.put(field, null);
		}

		return mappedFields;
	}

}
