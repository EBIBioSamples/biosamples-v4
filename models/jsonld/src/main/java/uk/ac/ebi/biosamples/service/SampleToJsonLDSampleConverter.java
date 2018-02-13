package uk.ac.ebi.biosamples.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.springframework.core.convert.converter.Converter;

import uk.ac.ebi.biosamples.model.*;

public class SampleToJsonLDSampleConverter implements Converter<Sample, JsonLDSample> {
    @Override
    public JsonLDSample convert(Sample sample) {


        JsonLDSample jsonLD = new JsonLDSample();
        jsonLD.setIdentifier(sample.getAccession());
        jsonLD.setName(sample.getName());

        List<JsonLDPropertyValue> jsonLDAttributeList = getAttributeList(sample);
        if (!jsonLDAttributeList.isEmpty()) {
            Optional<JsonLDPropertyValue> optionalDescription = jsonLDAttributeList.stream().filter(attr -> attr.getName().equalsIgnoreCase("description")).findFirst();
            if(optionalDescription.isPresent()) {
                JsonLDPropertyValue description = optionalDescription.get();
                jsonLD.setDescription(description.getValue());
                jsonLDAttributeList.remove(description);
            }
            jsonLD.setAdditionalProperties(jsonLDAttributeList);
        }

        List<String> datasets = getDatasets(sample);
        if (!datasets.isEmpty()) {
            jsonLD.setDataset(datasets);
        }
        return jsonLD;
    }

    private List<JsonLDPropertyValue> getAttributeList(Sample sample) {
        Iterator<Attribute> attributesIterator = sample.getAttributes().iterator();
        List<JsonLDPropertyValue> jsonLDAttributeList = new ArrayList<>();
        while(attributesIterator.hasNext()) {
            Attribute attr = attributesIterator.next();
            JsonLDPropertyValue pv = new JsonLDPropertyValue();
            pv.setName(attr.getType());
            pv.setValue(attr.getValue());
            if(attr.getIri().size() > 0) {
            	//this only puts the first IRI in
//                JsonLDMedicalCode medicalCode = new JsonLDMedicalCode();
//                medicalCode.setCodeValue(attr.getIri().iterator().next());

                // Assuming that if the iri is not starting with a http[s] is
                // probably a CURIE
                List<JsonLDCategoryCode> valueReferences = new ArrayList<>();
                for (String iri: attr.getIri()) {
                    JsonLDCategoryCode valueReference = new JsonLDCategoryCode();
                    if (iri.matches("^https?://.*")) {
                        valueReference.setUrl(iri);
                    } else {
                        valueReference.setCodeValue(iri);
                    }
                    valueReferences.add(valueReference);
                }
                pv.setValueReference(valueReferences);
            }
            jsonLDAttributeList.add(pv);
        }
        return jsonLDAttributeList;
    }

    private List<String> getDatasets(Sample sample) {
        Iterator<ExternalReference> externalRefsIterator = sample.getExternalReferences().iterator();
        List<String> datasets = new ArrayList<>();
        while(externalRefsIterator.hasNext()) {
            ExternalReference externalReference = externalRefsIterator.next();
            datasets.add(externalReference.getUrl());
        }
        return datasets;
    }
}
