package uk.ac.ebi.biosamples.model.ga4gh;

import java.util.Arrays;
import java.util.Objects;

public class Ga4ghIndividual {
    private String id;
    private String dataset_id;
    private String name;
    private String description;
    private Ga4ghBiocharacteristics[] bio_characteristics;
    private String created;
    private String updated;
    private Ga4ghOntologyTerm species;
    private Ga4ghOntologyTerm sex;
    private Ga4ghGeoLocation location;
    private Ga4ghAttributes attributes;
    private Ga4ghExternalIdentifier[] external_identifiers;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDataset_id() {
        return dataset_id;
    }

    public void setDataset_id(String dataset_id) {
        this.dataset_id = dataset_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Ga4ghBiocharacteristics[] getBio_characteristics() {
        return bio_characteristics;
    }

    public void setBio_characteristics(Ga4ghBiocharacteristics[] bio_characteristics) {
        this.bio_characteristics = bio_characteristics;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public Ga4ghOntologyTerm getSpecies() {
        return species;
    }

    public void setSpecies(Ga4ghOntologyTerm species) {
        this.species = species;
    }

    public Ga4ghOntologyTerm getSex() {
        return sex;
    }

    public void setSex(Ga4ghOntologyTerm sex) {
        this.sex = sex;
    }

    public Ga4ghGeoLocation getLocation() {
        return location;
    }

    public void setLocation(Ga4ghGeoLocation location) {
        this.location = location;
    }

    public Ga4ghAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(Ga4ghAttributes attributes) {
        this.attributes = attributes;
    }

    public Ga4ghExternalIdentifier[] getExternal_identifiers() {
        return external_identifiers;
    }

    public void setExternal_identifiers(Ga4ghExternalIdentifier[] external_identifiers) {
        this.external_identifiers = external_identifiers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ga4ghIndividual that = (Ga4ghIndividual) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(dataset_id, that.dataset_id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Arrays.equals(bio_characteristics, that.bio_characteristics) &&
                Objects.equals(created, that.created) &&
                Objects.equals(updated, that.updated) &&
                Objects.equals(species, that.species) &&
                Objects.equals(sex, that.sex) &&
                Objects.equals(location, that.location) &&
                Objects.equals(attributes, that.attributes) &&
                Arrays.equals(external_identifiers, that.external_identifiers);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(id, dataset_id, name, description, created, updated, species, sex, location, attributes);
        result = 31 * result + Arrays.hashCode(bio_characteristics);
        result = 31 * result + Arrays.hashCode(external_identifiers);
        return result;
    }

    @Override
    protected Ga4ghIndividual clone() {
        try {
            return (Ga4ghIndividual) super.clone();
        }catch (CloneNotSupportedException e){
            return new Ga4ghIndividual();
        }
    }
}
