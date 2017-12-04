package uk.ac.ebi.biosamples.legacy.json.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.*;
import org.springframework.core.convert.converter.Converter;
import uk.ac.ebi.biosamples.model.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.Entry;


public class JSONSampleToSampleConverter implements Converter<String, Sample> {

    private final ObjectMapper mapper;
    private ParseContext parseContext;

    public JSONSampleToSampleConverter() {
        this.mapper = new ObjectMapper();
        this.parseContext = JsonPath.using(getParseContextConfiguration());
    }

    private Configuration getParseContextConfiguration() {
        return Configuration.defaultConfiguration()
                .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);
    }

    @Override
    public Sample convert(String source) {
        DocumentContext jsonDoc = parseContext.parse(source);
        String accession = jsonDoc.read("$.accession");
        String sampleName = jsonDoc.read("$.name");
        String updateDate = jsonDoc.read("$.updateDate");
        String releaseDate = jsonDoc.read("$.releaseDate");
        String description = jsonDoc.read( "$.description");

        SortedSet<Attribute> attributes = getAttributes(jsonDoc);
        SortedSet<Attribute> submissionInfo = getSubmissionInformations(jsonDoc);
        SortedSet<Contact> contacts = getContacts(jsonDoc);
        SortedSet<Publication> publications = getPublications(jsonDoc);
        SortedSet<Organization> organizations = getOrganizations(jsonDoc);
        SortedSet<ExternalReference> embeddedExternalReferences = getEmbeddedExternalReferences(jsonDoc);

        Sample.Builder sampleBuilder = new Sample.Builder(accession, sampleName)
                .withUpdateDate(updateDate)
                .withReleaseDate(releaseDate)
                .withAttribute(Attribute.build("description", description));

        attributes.forEach(sampleBuilder::withAttribute);
        submissionInfo.forEach(sampleBuilder::withAttribute);
        contacts.forEach(sampleBuilder::withContact);
        publications.forEach(sampleBuilder::withPublication);
        organizations.forEach(sampleBuilder::withOrganization);
        embeddedExternalReferences.forEach(sampleBuilder::withExternalReference);

        if (accession.startsWith("SAMEG")) {
            getGroupRelationships(jsonDoc).forEach(sampleBuilder::withRelationship);
        }

        return sampleBuilder.build();

    }

    /**
     * Extract the has member relationships from the provided JSON string
     * @param json the JSON string to parse
     * @return a set of has Member relationships found in the JSON
     */
    private SortedSet<Relationship> getGroupRelationships(DocumentContext json) {
        SortedSet<Relationship> groupRelationships = new TreeSet<>();
        String groupAccession = json.read( "$.accession");
        List<String> groupMembers = json.read( "$.samples");
        for(String member: groupMembers) {
            groupRelationships.add(Relationship.build(groupAccession, "has member", member));
        }
        return groupRelationships;
    }

    /**
     * Extract the attributes (characteristics) from the provided JSON string
     * @param json the JSON string to parse
     * @return a set of Attributes found in the JSON
     */
    private SortedSet<Attribute> getAttributes(DocumentContext json) {
        LinkedHashMap<String, Object> characteristics = json.read( "$.characteristics");
        SortedSet<Attribute> attributes = new TreeSet<>();

        if (characteristics != null) {
            for (Entry<String, Object> crt : characteristics.entrySet()) {
                String attributeType = crt.getKey();
                List<Map<String, Object>> values = (List<Map<String, Object>>) crt.getValue();
                for (Map<String, Object> value : values) {

                    String attributeValue = (String) value.get("text");
                    List<String> ontologyTerms = (List<String>) value.get("ontologyTerms");
                    String unit = (String) value.get("unit");

                    attributes.add(Attribute.build(attributeType, attributeValue, ontologyTerms, unit));

                }


            }
        }
        return attributes;

    }

    /**
     * Extract the contacts from the provided JSON string
     * @param json the JSON string to parse
     * @return a set of Contacts found in the JSON
     */
    private SortedSet<Contact> getContacts(DocumentContext json) {
        List<Map<String, String>> contactList = json.read("$.contact");
        SortedSet<Contact> contacts = new TreeSet<>();
        if (contactList != null) {
            for(Map<String, String> contact: contactList) {
                contacts.add(new Contact.Builder().name(contact.get("Name")).build());
            }
        }
        return contacts;
    }

    /**
     * Extract the publications from the provided JSON string
     * @param json the JSON string to parse
     * @return a set of Publications found in the JSON
     */
    private SortedSet<Publication> getPublications(DocumentContext json) {
        List<Map<String, String>> publicationList = json.read("$.publications");
        SortedSet<Publication> publications = new TreeSet<>();
        if (publicationList != null) {
            for (Map<String, String> pub : publicationList) {
                publications.add(new Publication.Builder()
                        .doi(pub.get("doi"))
                        .pubmed_id(pub.get("pubmed_id"))
                        .build());
            }
        }
        return publications;
    }


    /**
     * Extract the organizations from the provided JSON string
     * @param json the JSON string to parse
     * @return the set of Organizations found in the JSON string
     */
    private SortedSet<Organization> getOrganizations(DocumentContext json) {
        List<Map<String, String>> organizationList = json.read( "$.organization");
        SortedSet<Organization> organizations = new TreeSet<>();
        if (organizationList != null) {
            for (Map<String, String> org : organizationList) {
                organizations.add(new Organization.Builder()
                        .name(org.get("Name"))
                        .role(org.get("Role"))
                        .address(org.get("Address"))
                        .email(org.get("Email"))
                        .url(org.get("URI"))
                        .build());
            }
        }
        return organizations;
    }

    /**
     * Extract submission informations form the provided JSON
     * @param json the JSON string to be parsed
     * @return the submission information available in the JSON as a set of Attributes
     */
    private SortedSet<Attribute> getSubmissionInformations(DocumentContext json) {
        String subAccession = json.read("$.submission_acc");
        String subTitle = json.read("$.submission_title");
        SortedSet<Attribute> submissionData = new TreeSet<>();

        if (subAccession != null )
            submissionData.add(Attribute.build("submission_acc", subAccession));

        if (subTitle != null)
            submissionData.add(Attribute.build("submission_title", subTitle));

        return submissionData;
    }

    /**
     * Extract the embedded external references from the JSON
     * @param json The JSON string to parse
     * @return the set of external resources found in the JSON
     */
    private SortedSet<ExternalReference> getEmbeddedExternalReferences(DocumentContext json) {
        SortedSet<ExternalReference> embeddedExternalReferences = new TreeSet<>();
        String serializedEmbeddedReferences = json.read("$.externalReferences");
        if (serializedEmbeddedReferences != null) {
            List<String> embeddedReferences = JsonPath.read(serializedEmbeddedReferences, "$..URL");
            if (embeddedReferences != null) {
                for (String url : embeddedReferences) {
                    embeddedExternalReferences.add(ExternalReference.build(url));
                }
            }
        }
        return embeddedExternalReferences;

    }

    private SortedSet<ExternalReference> getExternalReferences(String json) {
        //TODO not implemented because require the client to query a specific endpoint
        return new TreeSet<>();
    }

    private SortedSet<Relationship> getRelationships(String json) {
        //TODO not implemented because require the client to query a specific endpoint
        return new TreeSet<>();
    }
}