package uk.ac.ebi.biosamples.ebeye.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.ebeye.gen.*;
import uk.ac.ebi.biosamples.model.Sample;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class EbEyeBioSamplesDataDumpRunner implements ApplicationRunner {
    public static final String ENA_LC = "ena";
    public static final String ENA_UC = "ENA";
    @Autowired
    BioSamplesClient bioSamplesClient;

    public List<Sample> getSamplesList() {
        Iterable<Resource<Sample>> sampleResources = bioSamplesClient.fetchSampleResourceAll("NCBITaxon_2697049");
        List<Sample> sampleList = new ArrayList<>();

        sampleResources.forEach(sampleResource -> sampleList.add(sampleResource.getContent()));

        return sampleList;
    }

    public void convertSampleToXml(final List<Sample> samples, final File f) throws JAXBException {
        DatabaseType databaseType = new DatabaseType();

        databaseType.setName("BioSamples");
        databaseType.setDescription("EBI BioSamples Database");
        databaseType.setEntryCount(samples.size());
        databaseType.setRelease("BioSamples COVID-19 Samples Release");
        databaseType.setReleaseDate(new Date().toString());

        AtomicReference<EntriesType> entriesType = new AtomicReference<>(new EntriesType());

        samples.forEach(sample -> {
            entriesType.set(getEntries(sample, entriesType.get()));
            databaseType.setEntries(entriesType.get());
        });

        JAXBContext context = JAXBContext.newInstance(DatabaseType.class);

        Marshaller jaxbMarshaller = context.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        jaxbMarshaller.marshal(databaseType, f);
        jaxbMarshaller.marshal(databaseType, System.out);
    }

    private EntriesType getEntries(Sample sample, EntriesType entriesType) {
        EntryType entryType = new EntryType();

        getEntry(sample, entryType);
        entriesType.getEntry().add(entryType);

        return entriesType;
    }

    private void getEntry(Sample sample, EntryType entryType) {
        entryType.setId(sample.getAccession());
        entryType.setName(sample.getName());

        AdditionalFieldsType additionalFieldsType = new AdditionalFieldsType();

        getAdditionalFields(sample, entryType, additionalFieldsType);
        entryType.setAdditionalFields(additionalFieldsType);

        entryType.setDates(getDates(sample));
        entryType.setCrossReferences(getCrossReferences(sample));
    }

    private CrossReferencesType getCrossReferences(Sample sample) {
        CrossReferencesType crossReferencesType = new CrossReferencesType();

        sample.getExternalReferences().forEach(extRef -> {
            RefType refType = new RefType();

            final var url = extRef.getUrl();

            if (url.contains(ENA_LC) || url.contains(ENA_UC)) {
                refType.setDbname(ENA_UC);
                refType.setDbkey(extractEnaAccession(url));
            }

            crossReferencesType.getRef().add(refType);
        });

        crossReferencesType.getRef().add(getTaxonomyCrossReference());

        return crossReferencesType;
    }

    private RefType getTaxonomyCrossReference() {
        RefType refType = new RefType();

        refType.setDbname("TAXONOMY");
        refType.setDbkey("2697049");

        return refType;
    }

    private String extractEnaAccession(String url) {
        return url.substring(36);
    }

    private DatesType getDates(Sample sample) {
        DatesType datesType = new DatesType();
        DateType dateTypeRelease = new DateType();

        dateTypeRelease.setType("release_date");
        dateTypeRelease.setValue(sample.getReleaseDate());

        DateType dateTypeUpdate = new DateType();

        dateTypeUpdate.setType("update_date");
        dateTypeUpdate.setValue(sample.getUpdateDate());

        datesType.getDate().add(dateTypeRelease);
        datesType.getDate().add(dateTypeUpdate);

        return datesType;
    }

    private AdditionalFieldsType getAdditionalFields(Sample sample, EntryType entryType, AdditionalFieldsType additionalFieldsType) {
        sample.getAttributes().forEach(attribute -> {
            FieldType fieldType = new FieldType();

            if (attribute.getType().equals("description")) {
                entryType.setDescription(attribute.getValue());
            } else {
                fieldType.setName(removeOtherSpecialCharactersFromAttributeNames(removeSpacesFromAttributeNames(attribute.getType())));
                fieldType.setValue(attribute.getValue());
                additionalFieldsType.getFieldOrHierarchicalField().add(fieldType);
            }
        });

        return additionalFieldsType;
    }

    private String removeSpacesFromAttributeNames(String type) {
        return type.trim().replaceAll(" ", "_");
    }

    private String removeOtherSpecialCharactersFromAttributeNames(String type) {
        return type.trim().replaceAll("[^a-zA-Z0-9\\s+_-]", "");
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        File f = new File("C:\\Users\\dgupta\\biosd-dump.xml");
        List<Sample> samplesList = getSamplesList();

        convertSampleToXml(samplesList, f);
    }
}
