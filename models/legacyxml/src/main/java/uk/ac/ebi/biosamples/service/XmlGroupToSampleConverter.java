package uk.ac.ebi.biosamples.service;

import com.google.common.collect.Lists;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class XmlGroupToSampleConverter implements Converter<Element, Sample>  {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Override
	public Sample convert(Element doc) {
		
		Instant release = null;
		Instant update = null;
		String accession = null;
		if (XmlPathBuilder.of(doc).attributeExists("id")) {
			accession = XmlPathBuilder.of(doc).attribute("id");
		}
		String name = null;
		SortedSet<Attribute> attributes = new TreeSet<>();
		SortedSet<Relationship> relationships = new TreeSet<>();
		SortedSet<ExternalReference> externalReferences = new TreeSet<>();
		
		for (Element property : XmlPathBuilder.of(doc).elements("Property")){
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
				String value = XmlPathBuilder.of(property).path("QualifiedValue", "Value").text();
				Collection<String> iri = Lists.newArrayList();
				String unit = null;
				
				if (XmlPathBuilder.of(property).path("QualifiedValue", "TermSourceREF").exists()
						&& XmlPathBuilder.of(property).path("QualifiedValue", "TermSourceREF", "TermSourceID").exists()) {
					iri.add(XmlPathBuilder.of(property).path("QualifiedValue", "TermSourceREF", "TermSourceID").text());
				}
				
				if (XmlPathBuilder.of(property).path("QualifiedValue", "TermSourceREF").exists()
						&& XmlPathBuilder.of(property).path("QualifiedValue", "TermSourceREF", "Unit").exists()) {
					unit = XmlPathBuilder.of(property).path("QualifiedValue", "TermSourceREF", "Unit").text();
				}
				
				attributes.add(Attribute.build(type, value, iri, unit));				
			}
		}

		SortedSet<Contact> contacts = extractContacts(doc);
		SortedSet<Publication> publications = extractPublications(doc);
		SortedSet<Organization> organizations = extractOrganizations(doc);

		for (Element database : XmlPathBuilder.of(doc).elements("Database")){
			if (XmlPathBuilder.of(database).path("URI").exists()) {
				externalReferences.add(ExternalReference.build(XmlPathBuilder.of(database).path("URI").text()));
			}
		}
		
		log.info("name = "+name);
		log.info("accession = "+accession);
		log.info("release = "+release);
		log.info("update = "+update);
		log.info("attributes = "+attributes);
		log.info("relationships = "+relationships);
		log.info("relationships = "+relationships);
		
		return Sample.build(name, accession, null, release, update,
				attributes, relationships, externalReferences,
				organizations, contacts, publications);
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
