package uk.ac.ebi.biosamples.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.ga4gh.*;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Biosamples format to GA4GH format data converter
 *
 * @author Dilshat Salikhov
 */

@Service
public class SampleToGa4ghSampleConverter implements Converter<Sample, Ga4ghSample> {

    private Ga4ghSample baseBioSample;
    private GeoLocationDataHelper locationHelper;
    private Ga4ghSample ga4ghSample;

    @Autowired
    OLSDataRetriever olsRetriever;

    @Autowired
    public SampleToGa4ghSampleConverter(Ga4ghSample ga4ghSample, GeoLocationDataHelper helper) {
        this.baseBioSample = ga4ghSample;
        this.locationHelper = helper;
    }

    /**
     * Converts sample in Biosamples format to sample in GA4GH format
     *
     * @param rawSample sample retrieved from Biosamples
     * @return sample in GA4GH format
     */
    @Override
    public Ga4ghSample convert(Sample rawSample) {
        ga4ghSample = baseBioSample.clone();
        ga4ghSample.setId(rawSample.getAccession());
        ga4ghSample.setName(rawSample.getName());
        //TODO  dataset_id mapping
        mapCharacteristics(rawSample);
        mapRelationsihps(rawSample);
        mapRemainingData(rawSample);
        return ga4ghSample;
    }

    /**
     * Maps characteristics from Biosamples sample to GA4GH sample. Sorts data to: biocharacteristics, attributes,
     * geolocation info, age info,description, dataset_id.
     *
     * @param rawSample sample retrieved from Biosamples
     */
    private void mapCharacteristics(Sample rawSample) {
        SortedSet<Attribute> characteristics = rawSample.getCharacteristics();
        SortedSet<Attribute> locationInfo = new TreeSet<>();
        List<Attribute> attributes = new ArrayList<>();
        List<Attribute> bioCharacteristics = new ArrayList<>();
        for (Attribute attribute : characteristics) {
            String type = attribute.getType();
            if (type.equals("age")||type.equals("age_years")||type.equals("age(years)")){
                mapAge(attribute);
            } else if (locationHelper.isGeoLocationData(type)) {
                locationInfo.add(attribute);
            } else if (type.equals("individual")) {
                ga4ghSample.setIndividual_id(attribute.getValue());
            } else if (type.equals("dataset")) {
                ga4ghSample.setDataset_id(attribute.getValue());
            } else if (type.equals("description") || type.equals("description title")) {
                ga4ghSample.setDescription(attribute.getValue());
            } else {
                if (attribute.getValue() != null && attribute.getIriOls() != null) {
                    bioCharacteristics.add(attribute);
                } else {
                    attributes.add(attribute);
                }
            }

        }
        if (!locationInfo.isEmpty()) {
            mapLocation(locationInfo);
        }
        mapAttributes(attributes);
        mapBioCharacteristics(bioCharacteristics);
    }

    /**
     * Maps location data from biosamples to GA4GH GeoLocation.
     *
     * @param attributes geolocation data from biosample
     * @see GeoLocation
     */
    private void mapLocation(SortedSet<Attribute> attributes) {
        GeoLocation geoLocation = new GeoLocation();
        for (Attribute attribute : attributes) {
            switch (attribute.getType()) {
                case "geographic location":
                    geoLocation.setLabel(attribute.getValue());
                    break;
                case "location":
                    geoLocation.setLabel(attribute.getValue());
                    break;
                case "latitude and longitude":
                    Location location = locationHelper.convertToDecimalDegree(attribute.getValue());
                    geoLocation.setLatitude(location.getLatitude());
                    geoLocation.setLongtitude(location.getLongtitude());
                    break;
                case "latitude":
                    geoLocation.setLatitude(Double.parseDouble(attribute.getValue()));
                    break;
                case "longtitude":
                    geoLocation.setLongtitude((Double.parseDouble(attribute.getValue())));
                    break;
                case "altitude":
                    geoLocation.setAltitude(Double.parseDouble(attribute.getValue()));
                    break;
                case "precision":
                    geoLocation.setPrecision(attribute.getValue());
            }
        }

        ga4ghSample.setLocation(geoLocation);

    }

    /**
     * Maps relationships from Biosamples to ExternalIdentifiers in GA4GH.
     *
     * @param rawSample sample retreived from Biosamples
     * @see Relationship
     * @see ExternalIdentifier
     */
    private void mapRelationsihps(Sample rawSample) {
        SortedSet<ExternalIdentifier> externalIdentifiers = new TreeSet<>();
        for (Relationship relationship : rawSample.getRelationships()) {
            ExternalIdentifier identifier = new ExternalIdentifier();
            identifier.setRelation(relationship.getType());
            if (!relationship.getSource().equals(rawSample.getAccession())) {
                identifier.setIdentifier(relationship.getSource());
            } else {
                identifier.setIdentifier(relationship.getTarget());
            }
            externalIdentifiers.add(identifier);

        }
        ga4ghSample.setExternal_identifiers(externalIdentifiers);
    }

    /**
     * Maps age data from Biosamples to Age in GA4GH
     *
     * @param attribute attribute with age info from Biosamples
     * @see Age
     */
    private void mapAge(Attribute attribute) {
        Age age = new Age();
        age.setAge(attribute.getValue());
        try {
            SortedSet<String> iri = attribute.getIri();
            if (iri == null || iri.size() == 0) {
                age.setAge_class(null);
            } else {
                age.setAge_class(getSingleOntologyTerm(iri.first()));
            }
        } catch (NullPointerException e) {
            age.setAge_class(null);
        }
        ga4ghSample.setIndividual_age_at_collection(age);
    }

    /**
     * Maps characteristics from Biosamples to Attributes in GA4GH
     *
     * @param characteristics characteristics that provides nonbiological data (without Ontology term)
     * @see Attributes
     */
    private void mapAttributes(List<Attribute> characteristics) {
        characteristics.stream().forEach(attribute -> {
            List<AttributeValue> values = new ArrayList<>();
            AttributeValue value = new AttributeValue(attribute.getValue());
            values.add(value);
            ga4ghSample.addAttributeList(attribute.getType(), values);
        });

    }

    /**
     * Maps characteristics from Biosamples to Biocharacteristics in GA4GH
     *
     * @param characteristics characteristics that provides biological data (with Ontology term)
     * @see Attributes
     */
    private void mapBioCharacteristics(List<Attribute> characteristics) {
        SortedSet<Biocharacteristics> biocharacteristics = new TreeSet<>();

        characteristics.parallelStream().forEach(attribute -> {
            Biocharacteristics biocharacteristic = new Biocharacteristics();
            biocharacteristic.setDescription(attribute.getType());
            biocharacteristic.setScope(attribute.getUnit());
            biocharacteristic.setOntology_terms(getOntologyTerms(attribute.getIri()));
            biocharacteristics.add(biocharacteristic);
        });
        ga4ghSample.setBio_characteristic(biocharacteristics);

    }

    /**
     * Maps data that is not presented in Biosamples characteristics, but represents GA4GH Attributes:
     * contacts, released and updated dates, contacts, domain, external_references, organizations, publications.
     *
     * @param rawSample sample retreived from Biosamples
     */
    private void mapRemainingData(Sample rawSample) {
        ga4ghSample.addSingleAttributeValue("released", rawSample.getRelease().toString());
        ga4ghSample.addSingleAttributeValue("updated", rawSample.getUpdate().toString());
        ga4ghSample.addSingleAttributeValue("domain", rawSample.getDomain());
        if (!rawSample.getContacts().isEmpty()) {
            ga4ghSample.addAttributeList("contacts", convertObjectsToAttributeValues(rawSample.getContacts()));
        }
        if (!rawSample.getExternalReferences().isEmpty()) {
            ga4ghSample.addAttributeList("external_references", convertObjectsToAttributeValues(rawSample.getExternalReferences()));
        }
        if (!rawSample.getOrganizations().isEmpty()) {
            ga4ghSample.addAttributeList("organizations", convertObjectsToAttributeValues(rawSample.getOrganizations()));
        }
        if (!rawSample.getPublications().isEmpty()) {
            ga4ghSample.addAttributeList("publications", convertObjectsToAttributeValues(rawSample.getPublications()));
        }
    }

    /**
     * Retrieves ontology term by link from OLS lookup service
     *
     * @param link iri of term
     * @return retreived term
     * @see OLSDataRetriever
     */
    private OntologyTerm getSingleOntologyTerm(String link) {
        OLSDataRetriever retriever = new OLSDataRetriever();
        OntologyTerm term = new OntologyTerm();
        term.setUrl(link);
        retriever.readOntologyJsonFromUrl(link);
        term.setTerm_id(retriever.getOntologyTermId());
        term.setTerm_label(retriever.getOntologyTermLabel());
        return term;
    }

    /**
     * Retrieves ontology terms by set of links
     *
     * @param iris set of iris to ontology terms
     * @return set of Ontology terms
     * @see OntologyTerm
     */
    private SortedSet<OntologyTerm> getOntologyTerms(SortedSet<String> iris) {
        SortedSet<OntologyTerm> terms = new TreeSet<>();
        for (String link : iris) {
            OntologyTerm term = getSingleOntologyTerm(link);
            terms.add(term);
        }
        return terms;
    }

    /**
     * Converts Object list to ga4gh AttributeValue list
     *
     * @param values objects of any type
     * @param <T>
     * @return list of attributeValues (that contains values that supported by ga4gh)
     * @see AttributeValue
     */
    private <T> List<AttributeValue> convertObjectsToAttributeValues(SortedSet<T> values) {
        List<AttributeValue> attributes = new ArrayList<>();
        values.stream().forEach(value -> {
            SortedMap<String, Object> objectFieldsAndValues = null;
            try {
                objectFieldsAndValues = new TreeMap<>(getFieldNamesAndValues(value, false));
            } catch (IllegalAccessException e) {
                e.printStackTrace();

            }
            Attributes attributesFromField = new Attributes();
            SortedSet<String> namesOfFields = new TreeSet<>(objectFieldsAndValues.keySet());
            for (String key : namesOfFields) {

                String object = (String) objectFieldsAndValues.get(key);
                if (object != null) {
                    attributesFromField.addSingleAttribute(key, new AttributeValue(object));
                }
            }
            attributes.add(new AttributeValue(attributesFromField));
        });

        return attributes;

    }

    /**
     * Convert java objects to pairs of names and values of fields. This will be used in convertation that
     * to ga4gh format. From this pairs will be created AttributeValues that will be wrapped by Attributes.
     * @param obj object to read
     * @param publicOnly strict to not read non public fields
     * @return map of field names and values
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private Map<String, Object> getFieldNamesAndValues(final Object obj, boolean publicOnly)
            throws IllegalArgumentException, IllegalAccessException {
        Class<? extends Object> c1 = obj.getClass();
        Map<String, Object> map = new HashMap<String, Object>();
        Field[] fields = c1.getDeclaredFields();
        for (Field field : fields) {
            String name = field.getName();
            if (publicOnly) {
                if (Modifier.isPublic(field.getModifiers())) {
                    Object value = field.get(obj);
                    map.put(name, value);
                }
            } else {
                field.setAccessible(true);
                Object value = field.get(obj);
                map.put(name, value);
            }
        }
        return map;
    }


}
