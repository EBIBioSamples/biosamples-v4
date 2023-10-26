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
package uk.ac.ebi.biosamples.ebeye.base;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.ebeye.gen.*;
import uk.ac.ebi.biosamples.ebeye.util.AttributeLoader;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.AttributeFilter;
import uk.ac.ebi.biosamples.model.filter.DateRangeFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;

@Component
public class BioSamplesEbiSearchDataDumpRunner implements ApplicationRunner {
  private static final Logger log =
      LoggerFactory.getLogger(BioSamplesEbiSearchDataDumpRunner.class);
  private static final String ENA_LC = "ena";
  private static final String ENA_UC = "ENA";
  private static final int MAX_RETRY = 3;
  private final RefType taxonomyRefType = new RefType();
  @Autowired BioSamplesClient bioSamplesClient;
  @Autowired AttributeLoader attributeLoader;

  private Set<String> attributeSet;
  private Set<String> uniqueAccessionsSet;

  @Override
  public void run(final ApplicationArguments args) throws Exception {
    final boolean covidRun =
        !args.getOptionValues("COVID").iterator().next().equalsIgnoreCase("false");

    attributeSet = attributeLoader.getAllAttributes();
    uniqueAccessionsSet = new HashSet<>();

    if (covidRun) {
      final AtomicReference<String> startDate = new AtomicReference<>("");
      final AtomicReference<String> endDate = new AtomicReference<>("");

      final List<String> programArguments =
          args.getOptionNames().stream()
              .filter(optionName -> optionName.equals("startDate") || optionName.equals("endDate"))
              .toList();

      programArguments.forEach(
          programArgument -> {
            if (programArgument.equals("startDate")) {
              startDate.set(args.getOptionValues("startDate").get(0));
            }

            if (programArgument.equals("endDate")) {
              endDate.set(args.getOptionValues("endDate").get(0));
            }
          });

      log.info(
          "[COVID] Attempting to run for from date " + startDate.get() + " until " + endDate.get());
      log.info("[ORGANISM] batch");

      final LocalDate userSuppliedFromDate =
          LocalDate.parse(startDate.get(), DateTimeFormatter.ISO_LOCAL_DATE);
      final LocalDate userSuppliedToDate =
          LocalDate.parse(endDate.get(), DateTimeFormatter.ISO_LOCAL_DATE);

      LocalDate fromDate = LocalDate.parse(startDate.get(), DateTimeFormatter.ISO_LOCAL_DATE);
      LocalDate toDate = fromDate.plusDays(1);

      while (!fromDate.isEqual(userSuppliedToDate)) {
        log.info("[ORGANISM] Attempting to run for from date " + fromDate + " until " + toDate);

        final Collection<Filter> covidSamplesDateAndOrganismFilter = new ArrayList<>();

        covidSamplesDateAndOrganismFilter.add(getDateFilterCovid(fromDate, toDate));
        covidSamplesDateAndOrganismFilter.add(
            getAttributeFilter("organism", "Severe acute respiratory syndrome coronavirus 2"));

        createSampleExtracts(
            covidRun, covidSamplesDateAndOrganismFilter, "ORGANISM", fromDate, toDate);

        fromDate = toDate;
        toDate = toDate.plusDays(1);
      }

      log.info("[PROJECT] batch");
      log.info(
          "[PROJECT] Attempting to run for from date "
              + userSuppliedFromDate
              + " until "
              + userSuppliedToDate);

      final Collection<Filter> covidSamplesDateAndAttributeFilter = new ArrayList<>();

      covidSamplesDateAndAttributeFilter.add(
          getAttributeFilter("project name", "ReCoDID COVID-19 pilot study"));
      covidSamplesDateAndAttributeFilter.add(
          getDateFilterCovid(userSuppliedFromDate, userSuppliedToDate));

      createSampleExtracts(
          covidRun,
          covidSamplesDateAndAttributeFilter,
          "PROJECT",
          userSuppliedFromDate,
          userSuppliedToDate);
    }
  }

  private void createSampleExtracts(
      final boolean covidRun,
      final Collection<Filter> filterCollection,
      final String fileNameSpec,
      final LocalDate fromDate,
      final LocalDate toDate) {
    Iterable<EntityModel<Sample>> filteredCovidSamples = null;

    int retryCount = 0;

    while (retryCount <= MAX_RETRY) {
      try {
        filteredCovidSamples = bioSamplesClient.fetchSampleResourceAll("", filterCollection);

        if (filteredCovidSamples != null) {
          break;
        }
      } catch (final Exception e) {
        log.info("Failed for to get samples from BioSamples API, retrying");
        retryCount++;

        if (retryCount == MAX_RETRY) {
          log.info("Dumping failed - failed to get samples from BioSamples API");

          break;
        }
      }
    }

    if (filteredCovidSamples != null) {
      final int chunkSize = 1000;
      final AtomicInteger counter = new AtomicInteger();

      try {
        final Stream<EntityModel<Sample>> samplesStream =
            StreamSupport.stream(filteredCovidSamples.spliterator(), true);

        final Collection<List<EntityModel<Sample>>> splittedSamples =
            samplesStream
                .collect(
                    Collectors.groupingBy(enaSampleId -> counter.getAndIncrement() / chunkSize))
                .values();

        int iteration = 0;

        for (final List<EntityModel<Sample>> splittedSample : splittedSamples) {
          log.info("Processing iteration: " + ++iteration);

          final File extractFile =
              new File(
                  "C://Users//dgupta//COVID//"
                      + "biosd-dump_cv_19_"
                      + fileNameSpec
                      + fromDate
                      + toDate
                      + "_"
                      + iteration
                      + Math.random()
                      + ".xml");

          convertSampleToXml(
              getSamplesListCovid(splittedSample, fileNameSpec, uniqueAccessionsSet),
              extractFile,
              covidRun);
        }
      } catch (final Exception e) {
        e.printStackTrace();
        log.info("Failed for a batch " + fromDate + " and " + toDate);
      }
    }
  }

  private void convertSampleToXml(
      final List<Sample> samples, final File file, final boolean covidRun) throws JAXBException {
    final DatabaseType database = new DatabaseType();

    database.setName("BioSamples");
    database.setDescription("EBI BioSamples Database");
    database.setEntryCount(samples.size());
    database.setReleaseDate(new Date().toString());

    if (covidRun) {
      database.setRelease("BioSamples COVID Samples Release");
    } else {
      database.setRelease("BioSamples full Samples Release");
    }

    final AtomicReference<EntriesType> entriesType = new AtomicReference<>(new EntriesType());

    samples.forEach(
        sample -> {
          entriesType.set(getEntries(sample, entriesType.get(), covidRun));
          database.setEntries(entriesType.get());
        });

    final JAXBContext context = JAXBContext.newInstance(DatabaseType.class);

    final Marshaller jaxbMarshaller = context.createMarshaller();
    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

    jaxbMarshaller.marshal(database, file);
  }

  private EntriesType getEntries(
      final Sample sample, final EntriesType entriesType, final boolean covidRun) {
    final EntryType entryType = new EntryType();

    getEntry(sample, entryType, covidRun);
    entriesType.getEntry().add(entryType);

    return entriesType;
  }

  private void getEntry(final Sample sample, final EntryType entryType, final boolean covidRun) {
    entryType.setId(sample.getAccession());
    entryType.setName(sample.getName());

    final AdditionalFieldsType additionalFieldsType = new AdditionalFieldsType();

    if (covidRun) {
      getAdditionalFieldsCovid(sample, entryType, additionalFieldsType);
    } else {
      setTaxonomyReferenceType(sample);
      getAdditionalFields(sample, entryType, additionalFieldsType);
    }
    entryType.setAdditionalFields(additionalFieldsType);

    entryType.setDates(getDates(sample));
    entryType.setCrossReferences(getCrossReferences(sample, covidRun));
  }

  private void setTaxonomyReferenceType(final Sample sample) {
    if (sample.getTaxId() != null && sample.getTaxId() != 0) {
      taxonomyRefType.setDbname("TAXONOMY");
      taxonomyRefType.setDbkey(sample.getTaxId().toString());
    }
  }

  private CrossReferencesType getCrossReferences(final Sample sample, final boolean covidRun) {
    final CrossReferencesType crossReferencesType = new CrossReferencesType();

    sample
        .getExternalReferences()
        .forEach(
            extRef -> {
              final RefType refType = new RefType();

              final String url = extRef.getUrl();

              if (url.contains(ENA_LC) || url.contains(ENA_UC)) {
                refType.setDbname(ENA_UC);
                refType.setDbkey(extractEnaAccession(url));
              }

              crossReferencesType.getRef().add(refType);
            });

    sample
        .getRelationships()
        .forEach(
            relationship -> {
              final RefType refType = new RefType();

              refType.setDbname(
                  "BSD_RELATIONSHIP_"
                      + relationship.getType().trim().replaceAll(" ", "_").toUpperCase());
              refType.setDbkey(relationship.getSource());

              crossReferencesType.getRef().add(refType);
            });

    if (covidRun) {
      final Long taxId = sample.getTaxId();

      if (taxId != null) {
        crossReferencesType.getRef().add(getTaxonomyCrossReferenceCovid(taxId));
      }
    } else {
      crossReferencesType.getRef().add(taxonomyRefType);
    }

    return crossReferencesType;
  }

  private RefType getTaxonomyCrossReferenceCovid(final long taxId) {
    final RefType refType = new RefType();

    refType.setDbname("TAXONOMY");
    refType.setDbkey(String.valueOf(taxId));

    return refType;
  }

  private String extractEnaAccession(final String url) {
    return url.substring(39);
  }

  private DatesType getDates(final Sample sample) {
    final DatesType datesType = new DatesType();
    final DateType dateTypeRelease = new DateType();

    dateTypeRelease.setType("release_date");
    dateTypeRelease.setValue(sample.getReleaseDate());

    final DateType dateTypeUpdate = new DateType();

    dateTypeUpdate.setType("update_date");
    dateTypeUpdate.setValue(sample.getUpdateDate());

    datesType.getDate().add(dateTypeRelease);
    datesType.getDate().add(dateTypeUpdate);

    return datesType;
  }

  private void getAdditionalFields(
      final Sample sample,
      final EntryType entryType,
      final AdditionalFieldsType additionalFieldsType) {
    sample
        .getAttributes()
        .forEach(
            attribute -> {
              final FieldType fieldType = new FieldType();

              if (attribute.getType().equals("description")) {
                entryType.setDescription(attribute.getValue());
              } else {
                if (attributeSet.contains(attribute.getType())) {
                  fieldType.setName(
                      removeOtherSpecialCharactersFromAttributeNames(
                          removeSpacesFromAttributeNames(attribute.getType())));
                  fieldType.setValue(attribute.getValue());
                  additionalFieldsType.getFieldOrHierarchicalField().add(fieldType);
                }
              }
            });
  }

  private void getAdditionalFieldsCovid(
      final Sample sample,
      final EntryType entryType,
      final AdditionalFieldsType additionalFieldsType) {
    sample
        .getAttributes()
        .forEach(
            attribute -> {
              final FieldType fieldType = new FieldType();

              if (attribute.getType().equals("description")) {
                entryType.setDescription(attribute.getValue());
              } else {
                if (attribute.getType().equalsIgnoreCase("host")) {
                  fieldType.setName(
                      removeOtherSpecialCharactersFromAttributeNames(
                          removeSpacesFromAttributeNames("host_scientific_name")));
                  fieldType.setValue(attribute.getValue());
                  additionalFieldsType.getFieldOrHierarchicalField().add(fieldType);
                } else {
                  if (attributeSet.contains(attribute.getType())) {
                    fieldType.setName(
                        removeOtherSpecialCharactersFromAttributeNames(
                            removeSpacesFromAttributeNames(attribute.getType())));
                    fieldType.setValue(attribute.getValue());
                    additionalFieldsType.getFieldOrHierarchicalField().add(fieldType);
                  }
                }
              }
            });
  }

  private String removeSpacesFromAttributeNames(final String type) {
    return type.trim().replaceAll(" ", "_");
  }

  private String removeOtherSpecialCharactersFromAttributeNames(final String type) {
    return type.trim().replaceAll("[^a-zA-Z0-9\\s+_-]", "");
  }

  private static Filter getDateFilterCovid(final LocalDate fromDate, final LocalDate toDate) {
    return new DateRangeFilter.DateRangeFilterBuilder("update")
        .from(fromDate.atStartOfDay().toInstant(ZoneOffset.UTC))
        .until(toDate.atStartOfDay().toInstant(ZoneOffset.UTC))
        .build();
  }

  private static Filter getAttributeFilter(
      final String attributeName, final String attributeValue) {
    return new AttributeFilter.Builder(attributeName).withValue(attributeValue).build();
  }

  private List<Sample> getSamplesListCovid(
      final Iterable<EntityModel<Sample>> filteredCovidSamples,
      final String fileNameSpec,
      final Set<String> uniqueAccessionsSet) {
    final List<Sample> sampleList = new ArrayList<>();

    filteredCovidSamples.forEach(
        sampleResource -> {
          final Sample sample = sampleResource.getContent();
          final String accession = sample.getAccession();

          if (!uniqueAccessionsSet.contains(accession)) {
            sampleList.add(sample);
            uniqueAccessionsSet.add(accession);
          }
          log.info("Sample added for " + fileNameSpec + " " + accession);
        });

    return sampleList;
  }
}
