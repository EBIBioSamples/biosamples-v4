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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
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
public class EbEyeBioSamplesDataDumpRunner implements ApplicationRunner {
  private static Logger log = LoggerFactory.getLogger(EbEyeBioSamplesDataDumpRunner.class);
  /*private static final String BIOSAMPLES = "biosamples";
  private static final String MONGO_SAMPLE = "mongoSample";*/
  private static final String ENA_LC = "ena";
  private static final String ENA_UC = "ENA";
  private static final int MAX_RETRY = 3;
  private final RefType taxonomyRefType = new RefType();
  @Autowired BioSamplesClient bioSamplesClient;
  @Autowired AttributeLoader attributeLoader;

  /*@Value("${spring.data.mongodb.uri}")
  private String mongoUri;*/

  private Set<String> attributeSet;

  @Override
  public void run(final ApplicationArguments args) throws Exception {
    boolean covidRun = true;

    if (args.getOptionValues("COVID").iterator().next().equalsIgnoreCase("false")) {
      covidRun = false;
    }

    attributeSet = attributeLoader.getAllAttributes();

    if (covidRun) {
      final AtomicReference<String> startDate = new AtomicReference<>("");
      final AtomicReference<String> endDate = new AtomicReference<>("");

      final List<String> programArguments =
          args.getOptionNames().stream()
              .filter(optionName -> optionName.equals("startDate") || optionName.equals("endDate"))
              .collect(Collectors.toList());

      programArguments.forEach(
          programArgument -> {
            if (programArgument.equals("startDate")) {
              startDate.set(args.getOptionValues("startDate").get(0));
            }
            if (programArgument.equals("endDate")) {
              endDate.set(args.getOptionValues("endDate").get(0));
            }
          });

      final Collection<Filter> covidSamplesDateFilter =
          getDateFiltersCovid(startDate.get(), endDate.get());

      createSampleExtracts(
          covidRun, startDate, endDate, "NCBITaxon_2697049", covidSamplesDateFilter, "ONTOLOGY");

      final Collection<Filter> covidSamplesDateAndDiseaseTagFilter = new ArrayList<>();
      covidSamplesDateAndDiseaseTagFilter.add(
          new AttributeFilter.Builder("disease").withValue("COVID-19").build());
      covidSamplesDateAndDiseaseTagFilter.addAll(covidSamplesDateFilter);

      createSampleExtracts(
          covidRun, startDate, endDate, "", covidSamplesDateAndDiseaseTagFilter, "DISEASE_TAG");
    } else {
      /*final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      final MongoClientURI uri = new MongoClientURI(mongoUri);
      final MongoClient mongoClient = new MongoClient(uri);
      final DB db = mongoClient.getDB(BIOSAMPLES);
      final DBCollection coll = db.getCollection(MONGO_SAMPLE);
      final AtomicReference<String> startDate = new AtomicReference<>("");
      final String filePath = "";

      final List<String> programArguments =
          args.getOptionNames().stream()
              .filter(optionName -> optionName.equals("startDate"))
              .collect(Collectors.toList());

      programArguments.forEach(
          programArgument -> {
            if (programArgument.equals("startDate"))
              startDate.set(args.getOptionValues("startDate").get(0));
          });

      Date startDateFormatted = formatter.parse(String.valueOf(startDate));

      int fileCounter = 1;
      LocalDate startLocalDate = convertToLocalDateViaInstant(startDateFormatted);
      LocalDate endLocalDate = startLocalDate.plusMonths(1).minusDays(1);

      while (endLocalDate.isBefore(LocalDate.now())) {
        final File newFile = new File(filePath + "biosd_dump" + fileCounter++ + ".xml");

        log.info(
            "Running for samples with release date starting "
                + startLocalDate.toString()
                + " ending "
                + endLocalDate.toString()
                + " and writing to file "
                + newFile.getPath());

        fetchQueryAndDump(
            coll, startLocalDate.format(dtFormatter), endLocalDate.format(dtFormatter), newFile);

        startLocalDate = startLocalDate.plusMonths(1);
        endLocalDate = startLocalDate.plusMonths(1).minusDays(1);

        // if (fileCounter == 1) break;
      }*/
    }
  }

  private void createSampleExtracts(
      final boolean covidRun,
      final AtomicReference<String> startDate,
      final AtomicReference<String> endDate,
      final String filterText,
      final Collection<Filter> filterCollection,
      final String fileNameSpec)
      throws JAXBException {
    log.info("Running for covid from start date " + startDate.get() + " until " + endDate.get());

    boolean pagesRemaining = true;
    int pageProcessed = 0;
    Iterable<EntityModel<Sample>> filteredCovidSamples = null;

    while (pagesRemaining) {
      int retryCount = 0;

      while (retryCount <= MAX_RETRY) {
        try {
          filteredCovidSamples =
              bioSamplesClient.fetchPagedSampleResource(
                  filterText, filterCollection, pageProcessed++, 1000);

          if (filteredCovidSamples != null) {
            break;
          }
        } catch (final Exception e) {
          log.info("Failed for page " + pageProcessed + " retrying");
          retryCount++;

          if (retryCount == MAX_RETRY) {
            pageProcessed++;
          }
        }
      }

      if (filteredCovidSamples != null) {
        final long samplesInPage =
            StreamSupport.stream(filteredCovidSamples.spliterator(), false).count();

        log.info("Samples count for page " + pageProcessed + " is " + samplesInPage);

        if (samplesInPage > 0) {
          log.info("Samples remaining");

          final File extractFile =
              new File(
                  "/mnt/data/biosamples/data/ebeye/"
                      + "biosd-dump_cv_19_"
                      + fileNameSpec
                      + "_"
                      + pageProcessed
                      + Math.random()
                      + ".xml");
          final List<Sample> samplesList = getSamplesListCovid(filteredCovidSamples, fileNameSpec);

          convertSampleToXml(samplesList, extractFile, covidRun);
        } else {
          log.info("No more Samples remaining");

          pagesRemaining = false;
        }
      }
    }

    log.info("Dumping finished for this batch");
  }

  /* public LocalDate convertToLocalDateViaInstant(Date dateToConvert) {
      return dateToConvert.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
  */
  /*private void fetchQueryAndDump(
      final DBCollection coll, final String from, final String until, final File file)
      throws ParseException, JAXBException {
    final List<String> listOfAccessions = getAllDocuments(coll, from, until);

    log.info("Total number of samples to be dumped is : " + listOfAccessions.size());

    List<Sample> samplesList =
        listOfAccessions.stream().map(this::fetchSample).collect(Collectors.toList());

    convertSampleToXml(samplesList, file, false);
  }*/

  /*private static List<String> getAllDocuments(
      final DBCollection col, final String from, final String until) throws ParseException {
    final List<String> listOfAccessions = new ArrayList<>();
    final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    final Date startDate = simpleDateFormat.parse(from);
    final Date endDate = simpleDateFormat.parse(until);

    final BasicDBObject query =
        new BasicDBObject("release", new BasicDBObject("$gte", startDate).append("$lt", endDate));
    final DBCursor cursor =
        col.find(query).sort(new BasicDBObject("release", OrderBy.ASC.getIntRepresentation()));

    cursor.forEach(
        elem -> {
          final String accession = elem.get("_id").toString();

          log.info("Accession " + accession);
          listOfAccessions.add(elem.get("_id").toString());
        });

    return listOfAccessions;
  }*/

  /*public Sample fetchSample(final String accession) {
    Optional<Resource<Sample>> sampleResource = bioSamplesClient.fetchSampleResource(accession);

    return sampleResource.map(Resource::getContent).orElse(null);
  }*/

  public void convertSampleToXml(final List<Sample> samples, final File file, boolean covidRun)
      throws JAXBException {
    DatabaseType database = new DatabaseType();

    database.setName("BioSamples");
    database.setDescription("EBI BioSamples Database");
    database.setEntryCount(samples.size());
    database.setReleaseDate(new Date().toString());

    if (covidRun) {
      database.setRelease("BioSamples COVID Samples Release");
    } else {
      database.setRelease("BioSamples full Samples Release");
    }

    AtomicReference<EntriesType> entriesType = new AtomicReference<>(new EntriesType());

    samples.forEach(
        sample -> {
          entriesType.set(getEntries(sample, entriesType.get(), covidRun));
          database.setEntries(entriesType.get());
        });

    JAXBContext context = JAXBContext.newInstance(DatabaseType.class);

    Marshaller jaxbMarshaller = context.createMarshaller();
    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

    jaxbMarshaller.marshal(database, file);
  }

  private EntriesType getEntries(
      final Sample sample, final EntriesType entriesType, boolean covidRun) {
    final EntryType entryType = new EntryType();

    getEntry(sample, entryType, covidRun);
    entriesType.getEntry().add(entryType);

    return entriesType;
  }

  private void getEntry(final Sample sample, final EntryType entryType, boolean covidRun) {
    entryType.setId(sample.getAccession());
    entryType.setName(sample.getName());

    AdditionalFieldsType additionalFieldsType = new AdditionalFieldsType();

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

  private void setTaxonomyReferenceType(Sample sample) {
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
              RefType refType = new RefType();

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

  private RefType getTaxonomyCrossReferenceCovid(long taxId) {
    RefType refType = new RefType();

    refType.setDbname("TAXONOMY");
    refType.setDbkey(String.valueOf(taxId));

    return refType;
  }

  private String extractEnaAccession(final String url) {
    return url.substring(39);
  }

  private DatesType getDates(final Sample sample) {
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

  private void getAdditionalFields(
      final Sample sample,
      final EntryType entryType,
      final AdditionalFieldsType additionalFieldsType) {
    sample
        .getAttributes()
        .forEach(
            attribute -> {
              FieldType fieldType = new FieldType();

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
      Sample sample, EntryType entryType, AdditionalFieldsType additionalFieldsType) {
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

  private String removeSpacesFromAttributeNames(String type) {
    return type.trim().replaceAll(" ", "_");
  }

  private String removeOtherSpecialCharactersFromAttributeNames(String type) {
    return type.trim().replaceAll("[^a-zA-Z0-9\\s+_-]", "");
  }

  public static Collection<Filter> getDateFiltersCovid(final String from, final String to) {
    final LocalDate fromDate = LocalDate.parse(from, DateTimeFormatter.ISO_LOCAL_DATE);
    final LocalDate toDate = LocalDate.parse(to, DateTimeFormatter.ISO_LOCAL_DATE);

    final Filter dateFilter =
        new DateRangeFilter.DateRangeFilterBuilder("update")
            .from(fromDate.atStartOfDay().toInstant(ZoneOffset.UTC))
            .until(toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))
            .build();
    final Collection<Filter> filters = new ArrayList<>();
    filters.add(dateFilter);

    return filters;
  }

  private List<Sample> getSamplesListCovid(
      final Iterable<EntityModel<Sample>> filteredCovidSamples, final String fileNameSpec) {
    final List<Sample> sampleList = new ArrayList<>();

    filteredCovidSamples.forEach(
        sampleResource -> {
          final Sample sample = sampleResource.getContent();

          sampleList.add(sample);
          log.info("Sample added for " + fileNameSpec + " " + sample.getAccession());
        });

    return sampleList;
  }

  /*public List<Sample> getSamplesListCovid(
      AtomicReference<String> startDate, AtomicReference<String> endDate) {
    final List<Sample> sampleList = new ArrayList<>();
    final Collection<Filter> covidSamplesDateFilter =
        getDateFiltersCovid(startDate.get(), endDate.get());
    final Iterable<Resource<Sample>> textAndDateFilteredCovidSamples =
        bioSamplesClient.fetchSampleResourceAll("NCBITaxon_2697049", covidSamplesDateFilter);

    textAndDateFilteredCovidSamples.forEach(
        sampleResource -> {
          final Sample sample = sampleResource.getContent();

          sampleList.add(sample);
          log.info("Sample added for text and date match: " + sample.getAccession());
        });

    final Collection<Filter> covidSamplesDateAndDiseaseTagFilter = new ArrayList<>();
    covidSamplesDateAndDiseaseTagFilter.add(
        new AttributeFilter.Builder("disease").withValue("COVID-19").build());
    covidSamplesDateAndDiseaseTagFilter.addAll(covidSamplesDateFilter);

    final Iterable<Resource<Sample>> diseaseTagAndDateFilteredCovidSamples =
        bioSamplesClient.fetchSampleResourceAll(covidSamplesDateAndDiseaseTagFilter);

    diseaseTagAndDateFilteredCovidSamples.forEach(
        sampleResource -> {
          final Sample sample = sampleResource.getContent();

          sampleList.add(sample);
          log.info("Sample added for disease tag and date match: " + sample.getAccession());
        });

    return sampleList;
  }*/
}
