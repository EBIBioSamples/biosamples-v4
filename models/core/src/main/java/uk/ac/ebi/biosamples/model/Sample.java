package uk.ac.ebi.biosamples.model;


import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.service.CharacteristicDeserializer;
import uk.ac.ebi.biosamples.service.CharacteristicSerializer;
import uk.ac.ebi.biosamples.service.CustomInstantDeserializer;
import uk.ac.ebi.biosamples.service.CustomInstantSerializer;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"name", "accession", "domain", "release", "update", "taxId", "characteristics", "relationships", "externalReferences", "releaseDate", "updateDate", "submittedVia"})
public class Sample implements Comparable<Sample> {
    protected String accession;
    protected String name;

    /**
     * This is the unique permanent ID of the AAP domain/team
     * that owns this sample.
     */
    protected String domain;

    protected Instant release;
    protected Instant update;
    protected Instant create;

    protected SortedSet<Attribute> attributes;
    protected SortedSet<AbstractData> data;
    protected SortedSet<Relationship> relationships;
    protected SortedSet<ExternalReference> externalReferences;

    protected SortedSet<Organization> organizations;
    protected SortedSet<Contact> contacts;
    protected SortedSet<Publication> publications;
    protected SortedSet<Certificate> certificates;

    protected SubmittedViaType submittedVia;

    protected Sample() {

    }

    @JsonProperty("accession")
    public String getAccession() {
        return accession;
    }

    @JsonIgnore
    public boolean hasAccession() {
        if (accession != null && accession.trim().length() != 0) {
            return true;
        } else {
            return false;
        }
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("domain")
    public String getDomain() {
        return domain;
    }

    //DO NOT specify the JSON property value manually, must be autoinferred or errors
    @JsonSerialize(using = CustomInstantSerializer.class)
    public Instant getRelease() {
        return release;
    }

    //DO NOT specify the JSON property value manually, must be autoinferred or errors
    @JsonSerialize(using = CustomInstantSerializer.class)
    public Instant getUpdate() {
        return update;
    }

    @JsonSerialize(using = CustomInstantSerializer.class)
    public Instant getCreate() {
        return create;
    }

    @JsonProperty(value = "releaseDate", access = JsonProperty.Access.READ_ONLY)
    public String getReleaseDate() {
        return ZonedDateTime.ofInstant(release, ZoneOffset.UTC).format(ISO_LOCAL_DATE);
    }

    @JsonProperty(value = "updateDate", access = JsonProperty.Access.READ_ONLY)
    public String getUpdateDate() {
        return ZonedDateTime.ofInstant(update, ZoneOffset.UTC).format(ISO_LOCAL_DATE);
    }

    @JsonProperty(value = "taxId", access = JsonProperty.Access.READ_ONLY)
    public Integer getTaxId() {
        List<Integer> taxIds = new ArrayList<>();
        for (Attribute attribute : attributes) {
            if (attribute.getType().toLowerCase().equalsIgnoreCase("Organism") && !attribute.getIri().isEmpty()) {
                attribute.getIri().stream().
                        map(Object::toString).
                        map(this::extractTaxIdFromIri).
                        forEach(taxIds::add);
            }
        }
        if (taxIds.size() > 1) {
            return taxIds.get(0);
        }
        if (taxIds.size() == 0) {
            return 0;
        }
        return taxIds.get(0);
    }

    private int extractTaxIdFromIri(String iri) {
        if (iri.isEmpty()) return 0;
        String segments[] = iri.split("NCBITaxon_");
        try {
            return Integer.parseInt(segments[segments.length - 1]);
        } catch (NumberFormatException e) {
            return 0;
        }

    }

    @JsonIgnore
    public SortedSet<Attribute> getAttributes() {
        return attributes;
    }

    //DO NOT specify the JSON property value manually, must be autoinferred or errors
    @JsonSerialize(using = CharacteristicSerializer.class)
    public SortedSet<Attribute> getCharacteristics() {
        return attributes;
    }

    @JsonProperty("data")
    public SortedSet<AbstractData> getData() {
        return data;
    }

    @JsonProperty("relationships")
    public SortedSet<Relationship> getRelationships() {
        return relationships;
    }

    @JsonProperty("externalReferences")
    public SortedSet<ExternalReference> getExternalReferences() {
        return externalReferences;
    }

    @JsonProperty("organization")
    public SortedSet<Organization> getOrganizations() {
        return organizations;
    }

    @JsonProperty("contact")
    public SortedSet<Contact> getContacts() {
        return contacts;
    }

    @JsonProperty("publications")
    public SortedSet<Publication> getPublications() {
        return publications;
    }

    @JsonProperty("certificates")
    public SortedSet<Certificate> getCertificates() {
        return certificates;
    }

    @JsonProperty("submittedVia")
    public SubmittedViaType getSubmittedVia() {
        return submittedVia;
    }


    @Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof Sample)) {
            return false;
        }
        Sample other = (Sample) o;

        //dont use update date for comparisons, too volatile. SubmittedVia doesnt contain information for comparison

        return Objects.equals(this.name, other.name)
                && Objects.equals(this.accession, other.accession)
                && Objects.equals(this.domain, other.domain)
                && Objects.equals(this.release, other.release)
                && Objects.equals(this.attributes, other.attributes)
                && Objects.equals(this.data, other.data)
                && Objects.equals(this.relationships, other.relationships)
                && Objects.equals(this.externalReferences, other.externalReferences)
                && Objects.equals(this.organizations, other.organizations)
                && Objects.equals(this.contacts, other.contacts)
                && Objects.equals(this.publications, other.publications);
    }

    @Override
    public int compareTo(Sample other) {
        if (other == null) {
            return 1;
        }

        if (!this.accession.equals(other.accession)) {
            return this.accession.compareTo(other.accession);
        }

        if (!this.name.equals(other.name)) {
            return this.name.compareTo(other.name);
        }

        if (!this.release.equals(other.release)) {
            return this.release.compareTo(other.release);
        }

        if (!this.attributes.equals(other.attributes)) {
            if (this.attributes.size() < other.attributes.size()) {
                return -1;
            } else if (this.attributes.size() > other.attributes.size()) {
                return 1;
            } else {
                Iterator<Attribute> thisIt = this.attributes.iterator();
                Iterator<Attribute> otherIt = other.attributes.iterator();
                while (thisIt.hasNext() && otherIt.hasNext()) {
                    int val = thisIt.next().compareTo(otherIt.next());
                    if (val != 0) return val;
                }
            }
        }
        if (!this.relationships.equals(other.relationships)) {
            if (this.relationships.size() < other.relationships.size()) {
                return -1;
            } else if (this.relationships.size() > other.relationships.size()) {
                return 1;
            } else {
                Iterator<Relationship> thisIt = this.relationships.iterator();
                Iterator<Relationship> otherIt = other.relationships.iterator();
                while (thisIt.hasNext() && otherIt.hasNext()) {
                    int val = thisIt.next().compareTo(otherIt.next());
                    if (val != 0) return val;
                }
            }
        }
        if (!this.externalReferences.equals(other.externalReferences)) {
            if (this.externalReferences.size() < other.externalReferences.size()) {
                return -1;
            } else if (this.externalReferences.size() > other.externalReferences.size()) {
                return 1;
            } else {
                Iterator<ExternalReference> thisIt = this.externalReferences.iterator();
                Iterator<ExternalReference> otherIt = other.externalReferences.iterator();
                while (thisIt.hasNext() && otherIt.hasNext()) {
                    int val = thisIt.next().compareTo(otherIt.next());
                    if (val != 0) return val;
                }
            }
        }
        if (!this.organizations.equals(other.organizations)) {
            if (this.organizations.size() < other.organizations.size()) {
                return -1;
            } else if (this.organizations.size() > other.organizations.size()) {
                return 1;
            } else {
                Iterator<Organization> thisIt = this.organizations.iterator();
                Iterator<Organization> otherIt = other.organizations.iterator();
                while (thisIt.hasNext() && otherIt.hasNext()) {
                    int val = thisIt.next().compareTo(otherIt.next());
                    if (val != 0) return val;
                }
            }
        }
        if (!this.contacts.equals(other.contacts)) {
            if (this.contacts.size() < other.contacts.size()) {
                return -1;
            } else if (this.contacts.size() > other.contacts.size()) {
                return 1;
            } else {
                Iterator<Contact> thisIt = this.contacts.iterator();
                Iterator<Contact> otherIt = other.contacts.iterator();
                while (thisIt.hasNext() && otherIt.hasNext()) {
                    int val = thisIt.next().compareTo(otherIt.next());
                    if (val != 0) return val;
                }
            }
        }
        if (!this.publications.equals(other.publications)) {
            if (this.publications.size() < other.publications.size()) {
                return -1;
            } else if (this.publications.size() > other.publications.size()) {
                return 1;
            } else {
                Iterator<Publication> thisIt = this.publications.iterator();
                Iterator<Publication> otherIt = other.publications.iterator();
                while (thisIt.hasNext() && otherIt.hasNext()) {
                    int val = thisIt.next().compareTo(otherIt.next());
                    if (val != 0) return val;
                }
            }
        }
        return 0;
    }

    @Override
    public int hashCode() {
        //dont put update date in the hash because its not in comparison
        return Objects.hash(name, accession, release, attributes, data, relationships, externalReferences, organizations, publications);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Sample(");
        sb.append(name);
        sb.append(",");
        sb.append(accession);
        sb.append(",");
        sb.append(domain);
        sb.append(",");
        sb.append(release);
        sb.append(",");
        sb.append(update);
        sb.append(",");
        sb.append(create);
        sb.append(",");
        sb.append(attributes);
        sb.append(",");
        sb.append(relationships);
        sb.append(",");
        sb.append(externalReferences);
        sb.append(",");
        sb.append(organizations);
        sb.append(",");
        sb.append(contacts);
        sb.append(",");
        sb.append(publications);
        sb.append(",");
        sb.append(certificates);
        sb.append(",");
        sb.append(submittedVia);
        sb.append(")");
        return sb.toString();
    }

    public static Sample build(String name,
                               String accession,
                               String domain,
                               Instant release,
                               Instant update,
                               Instant create,
                               Set<Attribute> attributes,
                               Set<Relationship> relationships,
                               Set<ExternalReference> externalReferences) {
        return build(name, accession, domain, release, update, create, attributes, null,
                relationships, externalReferences, null, null, null, null, null);
    }

    public static Sample build(String name, String accession, String domain,
                               Instant release, Instant update, Instant create,
                               Set<Attribute> attributes, Set<Relationship> relationships,
                               Set<ExternalReference> externalReferences,
                               SubmittedViaType submittedVia) {
        return build(name, accession, domain, release, update, create, attributes, null,
                relationships, externalReferences, null, null, null, null, submittedVia);
    }

    //Used for deserializtion (JSON -> Java)
    @JsonCreator
    public static Sample build(@JsonProperty("name") String name,
                               @JsonProperty("accession") String accession,
                               @JsonProperty("domain") String domain,
                               @JsonProperty("release") @JsonDeserialize(using = CustomInstantDeserializer.class) Instant release,
                               @JsonProperty("update") @JsonDeserialize(using = CustomInstantDeserializer.class) Instant update,
                               @JsonProperty("create") @JsonDeserialize(using = CustomInstantDeserializer.class) Instant create,
                               @JsonProperty("characteristics") @JsonDeserialize(using = CharacteristicDeserializer.class) Collection<Attribute> attributes,
                               @JsonProperty("data")  Collection<AbstractData> structuredData,
                               @JsonProperty("relationships") Collection<Relationship> relationships,
                               @JsonProperty("externalReferences") Collection<ExternalReference> externalReferences,
                               @JsonProperty("organization") Collection<Organization> organizations,
                               @JsonProperty("contact") Collection<Contact> contacts,
                               @JsonProperty("publications") Collection<Publication> publications,
                               @JsonProperty("certificates") Collection<Certificate> certificates,
                               @JsonProperty("submittedVia") SubmittedViaType submittedVia) {

        Sample sample = new Sample();

        if (accession != null) {
            sample.accession = accession.trim();
        }

        if (name == null) throw new IllegalArgumentException("Sample name must be provided");
        sample.name = name.trim();

        if (domain != null) {
            sample.domain = domain.trim();
        }

        //Instead of validation failure, if null, set it to now
        sample.update = update == null ? Instant.now() : update;

        sample.create = create == null ? sample.update : create;

        //Validation moved to a later stage, to capture the error (SampleService.store())
        sample.release = release;

        sample.attributes = new TreeSet<>();
        if (attributes != null) {
            sample.attributes.addAll(attributes);
        }

        sample.relationships = new TreeSet<>();
        if (relationships != null) {
            sample.relationships.addAll(relationships);
        }

        sample.externalReferences = new TreeSet<>();
        if (externalReferences != null) {
            sample.externalReferences.addAll(externalReferences);
        }

        sample.organizations = new TreeSet<>();
        if (organizations != null) {
            sample.organizations.addAll(organizations);
        }

        sample.contacts = new TreeSet<>();
        if (contacts != null) {
            sample.contacts.addAll(contacts);
        }

        sample.publications = new TreeSet<>();
        if (publications != null) {
            sample.publications.addAll(publications);
        }

        sample.certificates = new TreeSet<>();
        if (certificates != null) {
            sample.certificates.addAll(certificates);
        }

        sample.data = new TreeSet<>();
        if (structuredData != null) {
            sample.data.addAll(structuredData);
        }

        if (submittedVia != null) {
            sample.submittedVia = submittedVia;
        } else {
            sample.submittedVia = SubmittedViaType.JSON_API;
        }

        return sample;
    }


    public static class Builder {

        protected String name;

        protected String accession = null;
        protected String domain = null;

        protected Instant release = Instant.now();
        protected Instant update = Instant.now();
        protected Instant create = Instant.now();

        protected SubmittedViaType submittedVia;

        protected SortedSet<Attribute> attributes = new TreeSet<>();
        protected SortedSet<Relationship> relationships = new TreeSet<>();
        protected SortedSet<ExternalReference> externalReferences = new TreeSet<>();
        protected SortedSet<Organization> organizations = new TreeSet<>();
        protected SortedSet<Contact> contacts = new TreeSet<>();
        protected SortedSet<Publication> publications = new TreeSet<>();
        protected SortedSet<Certificate> certificates = new TreeSet<>();
        protected Set<AbstractData> data = new TreeSet<>();

        public Builder(String name, String accession) {
            this.name = name;
            this.accession = accession;
        }

        public Builder(String name) {
            this.name = name;
        }

        public Builder withAccession(String accession) {
            this.accession = accession;
            return this;
        }

        public Builder withDomain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder withRelease(String release) {
            this.release = parseDateTime(release).toInstant();
            return this;
        }

        public Builder withRelease(Instant release) {
            this.release = release;
            return this;
        }

        public Builder withUpdate(Instant update) {
            this.update = update;
            return this;
        }

        public Builder withUpdate(String update) {
            this.update = parseDateTime(update).toInstant();
            return this;
        }

        public Builder withCreate(Instant create) {
            this.create = create;
            return this;
        }

        public Builder withCreate(String create) {
            this.create = parseDateTime(create).toInstant();
            return this;
        }

        public Builder withSubmittedVia(SubmittedViaType submittedVia) {
            this.submittedVia = submittedVia;
            return this;
        }

        /**
         * Replace builder's attributes with the provided attribute collection
         *
         * @param attributes
         * @return
         */
        public Builder withAttributes(Collection<Attribute> attributes) {
            this.attributes = new TreeSet<>(attributes);
            return this;
        }

        public Builder addAttribute(Attribute attribute) {
            this.attributes.add(attribute);
            return this;
        }

        public Builder addAllAttributes(Collection<Attribute> attributes) {
            this.attributes.addAll(attributes);
            return this;
        }

        /**
         * Replace builder structuredData with the provided structuredData collection
         *
         * @param data
         * @return
         */
        public Builder withData(Collection<AbstractData> data) {
            if (data != null) {
                this.data = new TreeSet<>(data);
            } else {
                this.data = new TreeSet<>();
            }
            return this;
        }

        public Builder addData(AbstractData data) {
            this.data.add(data);
            return this;
        }

        public Builder addAllData(Collection<AbstractData> data) {
            this.data.addAll(data);
            return this;
        }

        /**
         * Replace builder's relationships with the provided relationships collection
         *
         * @param relationships
         * @return
         */
        public Builder withRelationships(Collection<Relationship> relationships) {
            if (relationships != null)
                this.relationships = new TreeSet<>(relationships);
            return this;
        }

        public Builder addRelationship(Relationship relationship) {
            this.relationships.add(relationship);
            return this;
        }

        public Builder addAllRelationships(Collection<Relationship> relationships) {
            this.relationships.addAll(relationships);
            return this;
        }

        /**
         * Replace builder's externalReferences with the provided external references collection
         *
         * @param externalReferences
         * @return
         */
        public Builder withExternalReferences(Collection<ExternalReference> externalReferences) {
            if (externalReferences != null && externalReferences.size() > 0)
                this.externalReferences = new TreeSet<>(externalReferences);
            return this;
        }


        public Builder addExternalReference(ExternalReference externalReference) {
            this.externalReferences.add(externalReference);
            return this;
        }

        public Builder addAllExternalReferences(Collection<ExternalReference> externalReferences) {
            this.externalReferences.addAll(externalReferences);
            return this;
        }

        /**
         * Replace builder's organisations with the provided organisation collection
         *
         * @param organizations
         * @return
         */
        public Builder withOrganizations(Collection<Organization> organizations) {
            if (organizations != null && organizations.size() > 0)
                this.organizations = new TreeSet<>(organizations);
            return this;
        }

        public Builder addOrganization(Organization organization) {
            this.organizations.add(organization);
            return this;
        }

        public Builder allAllOrganizations(Collection<Organization> organizations) {
            this.organizations.addAll(organizations);
            return this;
        }

        /**
         * Replace builder's contacts with the provided contact collection
         *
         * @param contacts
         * @return
         */
        public Builder withContacts(Collection<Contact> contacts) {
            if (contacts != null && contacts.size() > 0)
                this.contacts = new TreeSet<>(contacts);
            return this;
        }

        public Builder addContact(Contact contact) {
            this.contacts.add(contact);
            return this;
        }

        public Builder addAllContacts(Collection<Contact> contacts) {
            this.contacts.addAll(contacts);
            return this;
        }

        /**
         * Replace the publications with the provided collections
         *
         * @param publications
         * @return
         */
        public Builder withPublications(Collection<Publication> publications) {
            if (publications != null && publications.size() > 0)
                this.publications = new TreeSet<>(publications);
            return this;
        }

        /**
         * Add a publication to the list of builder publications
         *
         * @param publication
         * @return
         */
        public Builder addPublication(Publication publication) {
            this.publications.add(publication);
            return this;
        }

        /**
         * Add all publications in the provided collection to the builder publications
         *
         * @param publications
         * @return
         */
        public Builder addAllPublications(Collection<Publication> publications) {
            this.publications.addAll(publications);
            return this;
        }

        public Builder withCertificates(Collection<Certificate> certificates) {
            if (certificates != null && certificates.size() > 0)
                this.certificates = new TreeSet<>(certificates);
            return this;
        }

        public Builder addAllCertificates(Collection<Certificate> certificates) {
            this.certificates.addAll(certificates);
            return this;
        }

        // Clean accession field
        public Builder withNoAccession() {
            this.accession = null;
            return this;
        }

        // Clean domain field
        public Builder withNoDomain() {
            this.domain = null;
            return this;
        }

        // Clean collection fields
        public Builder withNoAttributes() {
            this.attributes = new TreeSet<>();
            return this;
        }

        public Builder withNoRelationships() {
            this.relationships = new TreeSet<>();
            return this;
        }

        public Builder withNoData() {
            this.data = new TreeSet<>();
            return this;
        }

        public Builder withNoExternalReferences() {
            this.externalReferences = new TreeSet<>();
            return this;
        }

        public Builder withNoContacts() {
            this.contacts = new TreeSet<>();
            return this;
        }

        public Builder withNoOrganisations() {
            this.organizations = new TreeSet<>();
            return this;
        }

        public Builder withNoPublications() {
            this.publications = new TreeSet<>();
            return this;
        }

        public Sample build() {
            return Sample.build(name, accession, domain, release, update, create,
                    attributes, data, relationships, externalReferences,
                    organizations, contacts, publications, certificates, submittedVia);
        }

        private ZonedDateTime parseDateTime(String datetimeString) {
            if (datetimeString.isEmpty()) return null;
            TemporalAccessor temporalAccessor = getFormatter().parseBest(datetimeString,
                    ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
            if (temporalAccessor instanceof ZonedDateTime) {
                return (ZonedDateTime) temporalAccessor;
            } else if (temporalAccessor instanceof LocalDateTime) {
                return ((LocalDateTime) temporalAccessor).atZone(ZoneId.of("UTC"));
            } else {
                return ((LocalDate) temporalAccessor).atStartOfDay(ZoneId.of("UTC"));
            }

        }

        /**
         * Return a Builder produced extracting informations from the sample
         *
         * @param sample the sample to use as reference
         * @return the Builder
         */
        public static Builder fromSample(Sample sample) {
            return new Builder(sample.getName(), sample.getAccession()).withDomain(sample.getDomain())
                    .withRelease(sample.getRelease()).withUpdate(sample.getUpdate()).withCreate(sample.getCreate())
                    .withAttributes(sample.getAttributes()).withData(sample.getData())
                    .withRelationships(sample.getRelationships()).withExternalReferences(sample.getExternalReferences())
                    .withOrganizations(sample.getOrganizations()).withPublications(sample.getPublications()).withCertificates(sample.getCertificates())
                    .withContacts(sample.getContacts())
                    .withSubmittedVia(sample.getSubmittedVia());
        }

        private DateTimeFormatter getFormatter() {
            return new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .append(ISO_LOCAL_DATE)
                    .optionalStart()           // time made optional
                    .appendLiteral('T')
                    .append(ISO_LOCAL_TIME)
                    .optionalStart()           // zone and offset made optional
                    .appendOffsetId()
                    .optionalStart()
                    .appendLiteral('[')
                    .parseCaseSensitive()
                    .appendZoneRegionId()
                    .appendLiteral(']')
                    .optionalEnd()
                    .optionalEnd()
                    .optionalEnd()
                    .toFormatter();
        }
    }
}
