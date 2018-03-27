package uk.ac.ebi.biosamples.service;

import org.springframework.core.convert.converter.Converter;
import uk.ac.ebi.biosamples.model.*;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class SampleToJsonLDSampleRecordConverter implements Converter<Sample, JsonLDDataRecord> {

    @Override
    public JsonLDDataRecord convert(Sample sample) {

        JsonLDDataRecord sampleRecord = new JsonLDDataRecord();
        //TODO Check if we actually want to use release date as date created
        sampleRecord.dateCreated(sample.getRelease().atZone(ZoneId.of("UTC")));
        sampleRecord.dateModified(sample.getUpdate().atZone(ZoneId.of("UTC")));

        JsonLDSample jsonLD = new JsonLDSample();
        String[] identifiers = {getBioSamplesIdentifierDotOrg(sample.getAccession())};
        jsonLD.setIdentifiers(identifiers);
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

        sampleRecord.idetifier(getBioSamplesIdentifierDotOrg(sample.getAccession()));
        sampleRecord.mainEntity(jsonLD);

        return sampleRecord;
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

    private String getBioSamplesIdentifierDotOrg(String accession) {
        return "biosamples:"+accession;
    }

}
