package uk.ac.ebi.biosamples.service;

import com.google.common.base.Strings;
import org.dom4j.*;
import org.dom4j.tree.BaseElement;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SampleToXmlConverter implements Converter<Sample, Document> {
		
	private final Namespace xmlns = Namespace.get("http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0");
	private final Namespace xsi = Namespace.get("xsi", "http://www.w3.org/2001/XMLSchema-instance");
	private final ExternalReferenceService externalReferenceService;
	
	
	public SampleToXmlConverter(ExternalReferenceService externalReferenceService) {
		this.externalReferenceService = externalReferenceService;
	}
	
	@Override
	public Document convert(Sample source) {
        Document doc = DocumentHelper.createDocument();
        
		Element bioSample = doc.addElement("BioSample");
		
		bioSample.add(xmlns);
		bioSample.addAttribute("xsi:schemaLocation", "http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0 http://www.ebi.ac.uk/biosamples/assets/xsd/v1.0/BioSDSchema.xsd");
		bioSample.add(xsi);
		
		
		
		// 2012-04-15T23:00:00+00:00
		
		//DateTimeFormatter.ofPattern("yyyy-MM-ddTHH:mm:ss");
		
		
		bioSample.addAttribute("id", source.getAccession());
		bioSample.addAttribute("submissionUpdateDate", DateTimeFormatter.ISO_INSTANT.format(source.getUpdate()).replace("Z", "+00:00"));
		bioSample.addAttribute("submissionReleaseDate", DateTimeFormatter.ISO_INSTANT.format(source.getRelease()).replace("Z", "+00:00"));

		Element e = bioSample.addElement(QName.get("Property", xmlns));
		//Element e = parent.addElement("Property");
		e.addAttribute("class", "Sample Name");
		e.addAttribute("characteristic", "false");
		e.addAttribute("comment", "false");
		e.addAttribute("type", "STRING");		
		Element qv = e.addElement("QualifiedValue");
		Element v = qv.addElement("Value");
		v.addText(source.getName());
		
		//first create a temporary collections of information to allow sorting
		SortedMap<String, SortedSet<String>> attrTypeValue = new TreeMap<>();
		SortedMap<String, SortedMap<String, String>> attrIri = new TreeMap<>();
		SortedMap<String, SortedMap<String, String>> attrUnit = new TreeMap<>();
		
		for (Attribute attribute : source.getCharacteristics()) {

/*
	<Property class="Sample Name" characteristic="false" comment="false"
		type="STRING">
		<QualifiedValue>
			<Value>Test Sample</Value>
			<TermSourceREF>
				<Name />
				<TermSourceID>http://purl.obolibrary.org/obo/NCBITaxon_9606</TermSourceID>
			</TermSourceREF>
			<Unit>year</Unit>
		</QualifiedValue>
	</Property>		
 */		
			String attributeType = attribute.getType();
			if ("description".equals(attributeType)) {
				attributeType = "Sample Description";
			}
			if (!attrTypeValue.containsKey(attributeType)) {
				attrTypeValue.put(attributeType, new TreeSet<>());
				attrIri.put(attributeType, new TreeMap<>());
				attrUnit.put(attributeType, new TreeMap<>());
			}
			attrTypeValue.get(attributeType).add(attribute.getValue());
			
			if (attribute.getIri() != null && attribute.getIri().toString().length() > 0) {
				attrIri.get(attributeType).put(attribute.getValue(), attribute.getIri().toString());
			}

			if (attribute.getUnit() != null && attribute.getUnit().trim().length() > 0) {
				attrUnit.get(attributeType).put(attribute.getValue(), attribute.getUnit());
			}
		}
		
		for (String attributeType : attrTypeValue.keySet()) {		
			
			Element property = bioSample.addElement(QName.get("Property", xmlns));
			//Element e = parent.addElement("Property");
			property.addAttribute("class", attributeType);
			property.addAttribute("characteristic", "false");
			property.addAttribute("comment", "false");
			property.addAttribute("type", "STRING");		
			
			for (String attributeValue : attrTypeValue.get(attributeType)) {
				Element qualifiedValue = property.addElement("QualifiedValue");
				Element value = qualifiedValue.addElement("Value");
				value.addText(attributeValue);
				
				if (attrIri.get(attributeType).containsKey(attributeValue)) {
					Element termSourceRef = qualifiedValue.addElement("TermSourceREF");
					termSourceRef.addElement("Name");
					Element termSourceId = termSourceRef.addElement("TermSourceID");
					termSourceId.setText(attrIri.get(attributeType).get(attributeValue));					
				}
				
				if (attrUnit.get(attributeType).containsKey(attributeValue)) {
					Element unitE = qualifiedValue.addElement("Unit");
					unitE.setText(attrUnit.get(attributeType).get(attributeValue));					
				}
			}
		}
		//relationships other than derived from 
		for (Relationship relationship : source.getRelationships()) {
			if (!"derived from".equals(relationship.getType().toLowerCase())
					&& source.getAccession().equals(relationship.getSource())) {
				Element property = bioSample.addElement(QName.get("Property", xmlns));
				//Element e = parent.addElement("Property");
				property.addAttribute("class", relationship.getType());
				property.addAttribute("characteristic", "false");
				property.addAttribute("comment", "false");
				property.addAttribute("type", "STRING");		
				Element qualifiedValue = property.addElement("QualifiedValue");
				Element value = qualifiedValue.addElement("Value");
				value.addText(relationship.getTarget());
			}
		}
		
		//derivedFrom element
		for (Relationship relationship : source.getRelationships()) {
			if ("derived from".equals(relationship.getType().toLowerCase())
					&& source.getAccession().equals(relationship.getSource())) {
				Element derived = bioSample.addElement(QName.get("derivedFrom", xmlns));
				derived.setText(relationship.getTarget());
			}
		}
		
		for (ExternalReference externalReference : source.getExternalReferences()) {

			/*
			  <Database>
			    <Name>ENA</Name>
			    <URI>http://www.ebi.ac.uk/ena/data/view/ERS1463623</URI>
			    <ID>ERS1463623</ID>
			  </Database>
		 	*/	
			Element database = bioSample.addElement(QName.get("Database", xmlns));
			
			Element databaseName = database.addElement(QName.get("Name", xmlns));
			databaseName.setText(externalReferenceService.getNickname(externalReference));
			
			Element databaseUri = database.addElement(QName.get("URI", xmlns));
			databaseUri.setText(externalReference.getUrl());
			//use the last segment of the URI as the ID
			//not perfect, but good enough?
			List<String> pathSegments = UriComponentsBuilder.fromUriString(externalReference.getUrl()).build().getPathSegments();
			if (pathSegments.size() > 0) {
				Element databaseId = database.addElement(QName.get("ID", xmlns));
				databaseId.setText(pathSegments.get(pathSegments.size()-1));
			}
		}

		for (Contact contact: source.getContacts()) {
			Element person = new BaseElement(QName.get("Person", xmlns));

			if (!Strings.isNullOrEmpty(contact.getFirstName())) {
				Element personFirstName = person.addElement(QName.get("FirstName", xmlns));
				personFirstName.setText(contact.getFirstName());
			}

			if (!Strings.isNullOrEmpty(contact.getLastName())) {
				Element personLastName = person.addElement(QName.get("LastName", xmlns));
				personLastName.setText(contact.getLastName());

			}

			if (!Strings.isNullOrEmpty(contact.getMidInitials())) {
				Element personMidInitials = person.addElement(QName.get("MidInitials", xmlns));
				personMidInitials.setText(contact.getMidInitials());
			}

			if (!Strings.isNullOrEmpty(contact.getRole())) {
				Element personRole = person.addElement(QName.get("Role", xmlns));
				personRole.setText(contact.getRole());
			}

			if (!Strings.isNullOrEmpty(contact.getEmail())) {
				Element personEmail = person.addElement(QName.get("Email", xmlns));
				personEmail.setText(contact.getEmail());
			}

			if(person.hasContent()) {
				bioSample.add(person);
			}

		}

		for (Organization organization: source.getOrganizations()) {
			Element organizationElement = new BaseElement(QName.get("Organization", xmlns));

			if (!Strings.isNullOrEmpty(organization.getName())) {
				Element organizationName = organizationElement.addElement(QName.get("Name", xmlns));
				organizationName.setText(organization.getName());
			}

			if (!Strings.isNullOrEmpty(organization.getAddress())) {
				Element organizationAddress = organizationElement.addElement(QName.get("Address", xmlns));
				organizationAddress.setText(organization.getAddress());

			}

			if (!Strings.isNullOrEmpty(organization.getUrl())) {
				Element organizationURI = organizationElement.addElement(QName.get("URI", xmlns));
				organizationURI.setText(organization.getUrl());
			}

			if (!Strings.isNullOrEmpty(organization.getRole())) {
				Element organizationRole = organizationElement.addElement(QName.get("Role", xmlns));
				organizationRole.setText(organization.getRole());
			}

			if (!Strings.isNullOrEmpty(organization.getEmail())) {
				Element organizationEmail = organizationElement.addElement(QName.get("E-mail", xmlns));
				organizationEmail.setText(organization.getEmail());
			}

			if (organizationElement.hasContent()) {
				bioSample.add(organizationElement);
			}
		}

		for (Publication publication: source.getPublications()) {
			Element publicationElement = new BaseElement(QName.get("Publication", xmlns));

			if (!Strings.isNullOrEmpty(publication.getDoi())) {
				Element publicationDOI = publicationElement.addElement(QName.get("DOI", xmlns));
				publicationDOI.setText(publication.getDoi());
			}

			if (!Strings.isNullOrEmpty(publication.getPubMedId())) {
				Element publicationPubMedID = publicationElement.addElement(QName.get("PubMedID", xmlns));
				publicationPubMedID.setText(publication.getPubMedId());
			}

			if (publicationElement.hasContent()) {
				bioSample.add(publicationElement);
			}
		}

		return doc;
	}


}
