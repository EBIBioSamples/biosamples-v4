package uk.ac.ebi.biosamples.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.springframework.core.convert.converter.Converter;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.JsonLDMedicalCode;
import uk.ac.ebi.biosamples.model.JsonLDPropertyValue;
import uk.ac.ebi.biosamples.model.JsonLDSample;
import uk.ac.ebi.biosamples.model.Sample;

public class SampleToJsonLDSampleConverter implements Converter<Sample, JsonLDSample> {
    @Override
    public JsonLDSample convert(Sample sample) {


        JsonLDSample jsonLD = new JsonLDSample();
        jsonLD.setIdentifier(sample.getAccession());
        jsonLD.setName(sample.getName());

        List<JsonLDPropertyValue> jsonLDAttributeList = getAttributeList(sample);
        if (!jsonLDAttributeList.isEmpty()) {
            Optional<JsonLDPropertyValue> optionalDescription = jsonLDAttributeList.stream().filter(attr -> attr.getPropertyId().equalsIgnoreCase("description")).findFirst();
            if(optionalDescription.isPresent()) {
                JsonLDPropertyValue description = optionalDescription.get();
                jsonLD.setDescription(description.getValue());
                jsonLDAttributeList.remove(description);
            }
            jsonLD.setAdditionalProperties(jsonLDAttributeList);
        }

        List<String> datasetUrls = getDatasetUrls(sample);
        if (!datasetUrls.isEmpty()) {
            jsonLD.setDatasetUrl(datasetUrls);
        }
        return jsonLD;
    }

    private List<JsonLDPropertyValue> getAttributeList(Sample sample) {
        Iterator<Attribute> attributesIterator = sample.getAttributes().iterator();
        List<JsonLDPropertyValue> jsonLDAttributeList = new ArrayList<>();
        while(attributesIterator.hasNext()) {
            Attribute attr = attributesIterator.next();
            JsonLDPropertyValue pv = new JsonLDPropertyValue();
            pv.setPropertyId(attr.getType());
            pv.setValue(attr.getValue());
            if(attr.getIri().size() > 0) {
            	//this only puts the first IRI in
                JsonLDMedicalCode medicalCode = new JsonLDMedicalCode();
                medicalCode.setCodeValue(attr.getIri().iterator().next());
                pv.setCode(medicalCode);
            }
            jsonLDAttributeList.add(pv);
        }
        return jsonLDAttributeList;
    }

    private List<String> getDatasetUrls(Sample sample) {
        Iterator<ExternalReference> externalRefsIterator = sample.getExternalReferences().iterator();
        List<String> datasetUrls = new ArrayList<>();
        while(externalRefsIterator.hasNext()) {
            ExternalReference externalReference = externalRefsIterator.next();
            datasetUrls.add(externalReference.getUrl());
        }
        return datasetUrls;
    }
}
