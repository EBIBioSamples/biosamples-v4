package uk.ac.ebi.biosamples.legacy.json.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.*;

import java.util.*;
import java.util.Map.Entry;


@Service
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

        Sample.Builder sampleBuilder = new Sample.Builder(sampleName, accession)
                .withUpdate(updateDate)
                .withRelease(releaseDate)
                .withAttributes(attributes)
                .addAllAttributes(submissionInfo)
                .withContacts(contacts).withPublications(publications).withOrganizations(organizations)
                .withExternalReferences(embeddedExternalReferences);

        if (description != null && description.trim().length() > 0) {
            sampleBuilder.addAttribute(Attribute.build("description", description));
        }


        if (accession.startsWith("SAMEG")) {
            sampleBuilder.addAllRelationships(getGroupRelationships(jsonDoc));
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
        groupMembers = groupMembers == null ? new ArrayList<>() : groupMembers;
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
                if (contact.containsKey("Name")) {
                    contacts.add(new Contact.Builder().name(contact.get("Name")).build());
                }
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
        Object testValue = json.read("$.publications");
        SortedSet<Publication> publications = new TreeSet<>();
        if (testValue != null) {
            if (testValue instanceof String) {
                String serializedPublications = (String) testValue;
                List<String> dois = JsonPath.read(serializedPublications, "$..doi");
                List<String> pubmed_ids = JsonPath.read(serializedPublications, "$..pubmed_id");
                if (dois != null && pubmed_ids != null) {
                    int maxSize = Math.max(dois.size(), pubmed_ids.size());
                    for (int i=0; i < maxSize; i++) {
                        Publication.Builder publicationBuilder = new Publication.Builder();
                        if (dois.size() > i) {
                            publicationBuilder.doi(dois.get(i));
                        }
                        if (pubmed_ids.size() > i) {
                            publicationBuilder.pubmed_id(pubmed_ids.get(i));
                        }
                        publications.add(publicationBuilder.build());

                    }
                } else {
                    throw new RuntimeException("Unexpected format for publications " + serializedPublications);
                }
            } else {
                List<Map<String, String>> jsonObjets = (List<Map<String, String>>) testValue;
                for (Map<String, String> pub : jsonObjets) {
                    publications.add(new Publication.Builder().doi(pub.get("doi")).pubmed_id(pub.get("pubmed_id")).build());
                }

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
        Object testValue = json.read("$.externalReferences");
        if (testValue != null) {
            if (testValue instanceof String) {
                String serializedEmbeddedReferences = (String) testValue;
                List<String> embeddedReferences = JsonPath.read(serializedEmbeddedReferences, "$..URL");
                if (embeddedReferences != null) {
                    for (String url : embeddedReferences) {
                        embeddedExternalReferences.add(ExternalReference.build(url));
                    }
                }
            } else {
                List<Map<String, String>> externalReferences = (List<Map<String, String>>) testValue;
                for (Map<String, String> externalRef : externalReferences) {
                    embeddedExternalReferences.add(ExternalReference.build(externalRef.get("url")));
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