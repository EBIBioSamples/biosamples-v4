/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.service;

import com.google.common.base.Strings;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.tree.BaseElement;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Contact;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Organization;
import uk.ac.ebi.biosamples.model.Publication;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.StructuredDataEntry;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;

@Service
public class SampleToXmlConverter implements Converter<Sample, Document> {
  private final Namespace xmlns =
      Namespace.get("http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0");
  private final Namespace xsi = Namespace.get("xsi", "http://www.w3.org/2001/XMLSchema-instance");
  private final ExternalReferenceService externalReferenceService;

  public SampleToXmlConverter(ExternalReferenceService externalReferenceService) {
    this.externalReferenceService = externalReferenceService;
  }

  @Override
  public Document convert(Sample source) {
    if (source.getAccession() != null) {
      if (source.getAccession().startsWith("SAMEG")) {
        // its a group
        return sampleToBioSampleGroupXml(source);
      } else {
        return sampleToBioSampleXml(source);
      }
    } else {
      return null;
    }
  }

  private Document sampleToBioSampleXml(Sample source) {
    Document doc = DocumentHelper.createDocument();
    Element bioSample = doc.addElement("BioSample");

    bioSample.add(xmlns);
    bioSample.addAttribute(
        "xsi:schemaLocation",
        "http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0 http://www.ebi.ac.uk/biosamples/assets/xsd/v1.0/BioSDSchema.xsd");
    bioSample.add(xsi);

    // 2012-04-15T23:00:00+00:00

    // DateTimeFormatter.ofPattern("yyyy-MM-ddTHH:mm:ss");
    bioSample.addAttribute("id", source.getAccession());
    bioSample.addAttribute(
        "submissionCreateDate",
        DateTimeFormatter.ISO_INSTANT.format(source.getCreate()).replace("Z", "+00:00"));

    bioSample.addAttribute(
        "submissionUpdateDate",
        DateTimeFormatter.ISO_INSTANT.format(source.getUpdate()).replace("Z", "+00:00"));

    if (source.getSubmitted() != null) {
      bioSample.addAttribute(
          "submissionSubmitDate",
          DateTimeFormatter.ISO_INSTANT.format(source.getSubmitted()).replace("Z", "+00:00"));
    }

    bioSample.addAttribute(
        "submissionReleaseDate",
        DateTimeFormatter.ISO_INSTANT.format(source.getRelease()).replace("Z", "+00:00"));

    addName(source, "Sample Name", bioSample);

    // first create a temporary collections of information to allow sorting
    SortedMap<String, SortedSet<String>> attributeTypesAndValuesMap = new TreeMap<>();
    SortedMap<String, SortedMap<String, String>> attributeIris = new TreeMap<>();
    SortedMap<String, SortedMap<String, String>> attributeUnits = new TreeMap<>();

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
    handleAttribute(source, attributeTypesAndValuesMap, attributeIris, attributeUnits);

    // relationships other than derived from

    for (Relationship relationship : source.getRelationships()) {
      if (!"derived from".equals(relationship.getType().toLowerCase())
          && source.getAccession().equals(relationship.getSource())) {

        if (!attributeTypesAndValuesMap.containsKey(relationship.getType())) {
          attributeTypesAndValuesMap.put(relationship.getType(), new TreeSet<>());
          attributeIris.put(relationship.getType(), new TreeMap<>());
          attributeUnits.put(relationship.getType(), new TreeMap<>());
        }
        attributeTypesAndValuesMap.get(relationship.getType()).add(relationship.getTarget());
      }
    }

    handleAttributes(bioSample, attributeTypesAndValuesMap, attributeIris, attributeUnits);

    // derivedFrom element
    for (Relationship relationship : source.getRelationships()) {
      if ("derived from".equals(relationship.getType().toLowerCase())
          && source.getAccession().equals(relationship.getSource())) {
        Element derived = bioSample.addElement(QName.get("derivedFrom", xmlns));
        derived.setText(relationship.getTarget());
      }
    }

    /*
     <Database>
       <Name>ENA</Name>
       <URI>http://www.ebi.ac.uk/ena/data/view/ERS1463623</URI>
       <ID>ERS1463623</ID>
     </Database>
    */
    handleExternalReferencesConversion(source, bioSample);

    for (StructuredDataTable data : source.getStructuredData()) {
      Element dataParent =
          bioSample.addElement(QName.get("Table", xmlns)).addAttribute("name", data.getType());

      Element dataHeader = dataParent.addElement(QName.get("Header", xmlns));
      for (String key : data.getHeaders()) {
        dataHeader.addElement(QName.get("Cell", xmlns)).setText(key);
      }

      Element dataBody = dataParent.addElement(QName.get("Body", xmlns));
      for (Map<String, StructuredDataEntry> row : data.getContent()) {
        Element dataRow = dataBody.addElement(QName.get("Row", xmlns));
        for (String key : data.getHeaders()) {
          dataRow
              .addElement(QName.get("Cell", xmlns))
              .setText(row.containsKey(key) ? row.get(key).getValue() : "");
        }
      }
    }

    return doc;
  }

  private void handleAttribute(
      Sample source,
      SortedMap<String, SortedSet<String>> attrTypeValue,
      SortedMap<String, SortedMap<String, String>> attrIri,
      SortedMap<String, SortedMap<String, String>> attrUnit) {
    for (Attribute attribute : source.getCharacteristics()) {
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

      if (attribute.getIri() != null && attribute.getIri().size() > 0) {
        attrIri.get(attributeType).put(attribute.getValue(), attribute.getIri().first());
      }

      if (attribute.getUnit() != null && attribute.getUnit().trim().length() > 0) {
        attrUnit.get(attributeType).put(attribute.getValue(), attribute.getUnit());
      }
    }
  }

  private void handleExternalReferencesConversion(Sample source, Element bioSample) {
    for (ExternalReference externalReference : source.getExternalReferences()) {
      Element database = bioSample.addElement(QName.get("Database", xmlns));
      Element databaseName = database.addElement(QName.get("Name", xmlns));
      databaseName.setText(externalReferenceService.getNickname(externalReference));
      Element databaseUri = database.addElement(QName.get("URI", xmlns));
      databaseUri.setText(externalReference.getUrl());
      // use the last segment of the URI as the ID
      // not perfect, but good enough?
      List<String> pathSegments =
          UriComponentsBuilder.fromUriString(externalReference.getUrl()).build().getPathSegments();

      if (pathSegments.size() > 0) {
        Element databaseId = database.addElement(QName.get("ID", xmlns));
        databaseId.setText(pathSegments.get(pathSegments.size() - 1));
      }
    }
  }

  private void addName(Sample source, String fieldname, Element bioSample) {
    Element e = bioSample.addElement(QName.get("Property", xmlns));
    e.addAttribute("class", fieldname);
    e.addAttribute("characteristic", "false");
    e.addAttribute("comment", "false");
    e.addAttribute("type", "STRING");
    Element qv = e.addElement("QualifiedValue");
    Element v = qv.addElement("Value");
    v.addText(source.getName());
  }

  private Document sampleToBioSampleGroupXml(Sample source) {
    Document doc = DocumentHelper.createDocument();
    Element bioSample = doc.addElement("BioSampleGroup");

    bioSample.add(xmlns);
    bioSample.addAttribute(
        "xsi:schemaLocation",
        "http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0 http://www.ebi.ac.uk/biosamples/assets/xsd/v1.0/BioSDSchema.xsd");
    bioSample.add(xsi);

    // 2012-04-15T23:00:00+00:00

    // DateTimeFormatter.ofPattern("yyyy-MM-ddTHH:mm:ss");

    bioSample.addAttribute("id", source.getAccession());
    addName(source, "Group Name", bioSample);

    // first create a temporary collections of information to allow sorting
    SortedMap<String, SortedSet<String>> attributeTypesAndValuesMap = new TreeMap<>();
    SortedMap<String, SortedMap<String, String>> attributeIris = new TreeMap<>();
    SortedMap<String, SortedMap<String, String>> attributeUnits = new TreeMap<>();

    // release and update date
    attributeTypesAndValuesMap.put("Submission Release Date", new TreeSet<>());
    attributeIris.put("Submission Release Date", new TreeMap<>());
    attributeUnits.put("Submission Release Date", new TreeMap<>());
    attributeTypesAndValuesMap
        .get("Submission Release Date")
        .add(DateTimeFormatter.ISO_INSTANT.format(source.getRelease()).replace("Z", "+00:00"));

    attributeTypesAndValuesMap.put("Submission Update Date", new TreeSet<>());
    attributeIris.put("Submission Update Date", new TreeMap<>());
    attributeUnits.put("Submission Update Date", new TreeMap<>());
    attributeTypesAndValuesMap
        .get("Submission Update Date")
        .add(DateTimeFormatter.ISO_INSTANT.format(source.getUpdate()).replace("Z", "+00:00"));

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
    handleAttribute(source, attributeTypesAndValuesMap, attributeIris, attributeUnits);

    handleAttributes(bioSample, attributeTypesAndValuesMap, attributeIris, attributeUnits);

    for (Contact contact : source.getContacts()) {
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

      if (person.hasContent()) {
        bioSample.add(person);
      }
    }

    for (Organization organization : source.getOrganizations()) {
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

    for (Publication publication : source.getPublications()) {
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

    handleExternalReferencesConversion(source, bioSample);

    return doc;
  }

  private void handleAttributes(
      Element bioSample,
      SortedMap<String, SortedSet<String>> attrTypeValue,
      SortedMap<String, SortedMap<String, String>> attrIri,
      SortedMap<String, SortedMap<String, String>> attrUnit) {
    for (String attributeType : attrTypeValue.keySet()) {
      Element property = bioSample.addElement(QName.get("Property", xmlns));
      // Element e = parent.addElement("Property");
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
  }
}
