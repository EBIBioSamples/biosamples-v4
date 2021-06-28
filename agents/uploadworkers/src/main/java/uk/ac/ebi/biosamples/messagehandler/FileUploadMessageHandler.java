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
package uk.ac.ebi.biosamples.messagehandler;

import com.google.common.collect.Multimap;
import com.mongodb.gridfs.GridFSDBFile;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Contact;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.upload.Characteristics;
import uk.ac.ebi.biosamples.utils.upload.FileUploadUtils;
import uk.ac.ebi.biosamples.utils.upload.ValidationResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FileUploadMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());
    private ValidationResult validationResult;
    private FileUploadUtils fileUploadUtils;

    @Autowired
    BioSamplesClient bioSamplesAapClient;

    @Autowired
    @Qualifier("WEBINCLIENT")
    BioSamplesClient bioSamplesWebinClient;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @RabbitListener(queues = Messaging.fileUploadQueue, containerFactory = "containerFactory")
    public void receiveMessageFromBioSamplesFileUploaderQueue(String mongoFileId) {
        handleMessage(mongoFileId);
    }

    private synchronized void handleMessage(String mongoFileId) {
        try {
            validationResult = new ValidationResult();
            fileUploadUtils = new FileUploadUtils();

            log.info(
                    "File with mongo file id "
                            + mongoFileId);

            final GridFSDBFile file = getUploadedFile(mongoFileId);

            // get file metadata, determine webin aur aap auth and use client accordingly
            String aapDomain = file.getMetaData().toMap().get("aap_domain").toString();
            String webinId = file.getMetaData().toMap().get("webin_id").toString();
            String checklist = file.getMetaData().toMap().get("certificate").toString();
            boolean isWebin = isWebinIdUsedToAuthenticate(webinId);

            final Path temp = Files.createTempFile("upload", ".tsv");

            Files.copy(
                    file.getInputStream(),
                    temp,
                    StandardCopyOption.REPLACE_EXISTING);

            File fileToBeUploaded = temp.toFile();
            FileReader fr = new FileReader(fileToBeUploaded);
            BufferedReader reader = new BufferedReader(fr);

            final CSVParser csvParser = fileUploadUtils.buildParser(reader);
            final List<Multimap<String, String>> csvDataMap = fileUploadUtils.getCSVDataInMap(csvParser);

            log.info("CSV data size: " + csvDataMap.size());

            final List<Sample> samples = buildSamples(csvDataMap, aapDomain, webinId, checklist, validationResult, isWebin);

            final String persistenceMessage = "Number of samples persisted: " + samples.size();

            log.info(persistenceMessage);
            validationResult.addValidationMessage(persistenceMessage);
            log.info(String.join("\n", validationResult.getValidationMessagesList()));
        } catch (Exception e) {
            final String messageForBsdDevTeam =
                    "********FEEDBACK TO BSD DEV TEAM START********"
                            + e.getMessage()
                            + "********FEEDBACK TO BSD DEV TEAM END********";
            validationResult.addValidationMessage(messageForBsdDevTeam);
            throw new RuntimeException(
                    String.join("\n", validationResult.getValidationMessagesList()));
        } finally {
            validationResult.clear();
        }
    }

    private List<Sample> buildSamples(
            List<Multimap<String, String>> csvDataMap,
            String aapDomain,
            String webinId,
            String checklist,
            ValidationResult validationResult,
            boolean isWebin) {
        final Map<String, String> sampleNameToAccessionMap = new LinkedHashMap<>();
        final Map<Sample, Multimap<String, String>> sampleToMappedSample = new LinkedHashMap<>();

        csvDataMap.forEach(
                csvRecordMap -> {
                    Sample sample = null;

                    try {
                        sample =
                                buildSample(
                                        csvRecordMap, aapDomain, webinId, checklist, isWebin);

                        if (sample == null) {
                            validationResult.addValidationMessage("Failed to create all samples in the file");
                        }
                    } catch (Exception e) {
                        validationResult.addValidationMessage("Failed to create all samples in the file");
                    }

                    if (sample != null) {
                        sampleNameToAccessionMap.put(sample.getName(), sample.getAccession());
                        sampleToMappedSample.put(sample, csvRecordMap);
                    }
                });

        return addRelationshipsAndThenBuildSamples(
                sampleNameToAccessionMap, sampleToMappedSample, validationResult, isWebin);
    }

    private boolean isWebinIdUsedToAuthenticate(String webinId) {
        return webinId != null && webinId.toUpperCase().startsWith("WEBIN");
    }

    private List<Sample> addRelationshipsAndThenBuildSamples(
            Map<String, String> sampleNameToAccessionMap,
            Map<Sample, Multimap<String, String>> sampleToMappedSample,
            ValidationResult validationResult,
            boolean isWebin) {
        return sampleToMappedSample.entrySet().stream()
                .map(
                        sampleMultimapEntry ->
                                addRelationshipAndThenBuildSample(
                                        sampleNameToAccessionMap, sampleMultimapEntry, validationResult, isWebin))
                .collect(Collectors.toList());
    }

    private Sample addRelationshipAndThenBuildSample(
            Map<String, String> sampleNameToAccessionMap,
            Map.Entry<Sample, Multimap<String, String>> sampleMultimapEntry,
            ValidationResult validationResult,
            boolean isWebin) {
        final Multimap<String, String> relationshipMap =
                fileUploadUtils.parseRelationships(sampleMultimapEntry.getValue());
        Sample sample = sampleMultimapEntry.getKey();
        final List<Relationship> relationships =
                fileUploadUtils.createRelationships(sample, sampleNameToAccessionMap, relationshipMap, validationResult);

        relationships.forEach(relationship -> log.trace(relationship.toString()));

        sample = Sample.Builder.fromSample(sample).withRelationships(relationships).build();

        if (isWebin) {
            sample = bioSamplesWebinClient.persistSampleResource(sample).getContent();
        } else {
            sample = bioSamplesAapClient.persistSampleResource(sample).getContent();
        }

        return sample;
    }

    private Sample buildSample(
            Multimap<String, String> multiMap,
            String aapDomain,
            String webinId,
            String checklist,
            boolean isWebin) {
        final String sampleName = fileUploadUtils.getSampleName(multiMap);
        final String sampleReleaseDate = fileUploadUtils.getReleaseDate(multiMap);
        final String accession = fileUploadUtils.getSampleAccession(multiMap);
        final List<Characteristics> characteristicsList = fileUploadUtils.handleCharacteristics(multiMap);
        final List<ExternalReference> externalReferenceList = fileUploadUtils.handleExternalReferences(multiMap);
        final List<Contact> contactsList = fileUploadUtils.handleContacts(multiMap);

        if (fileUploadUtils.isBasicValidationFailure(sampleName, sampleReleaseDate, validationResult)) {
            Sample sample = fileUploadUtils.buildSample(sampleName, accession, characteristicsList, externalReferenceList, contactsList);

            sample = fileUploadUtils.addChecklistAttributeAndBuildSample(checklist, sample);

            if (isWebin) {
                sample = Sample.Builder.fromSample(sample).withWebinSubmissionAccountId(webinId).build();
                sample = bioSamplesWebinClient.persistSampleResource(sample).getContent();
            } else {
                sample = Sample.Builder.fromSample(sample).withDomain(aapDomain).build();
                sample = bioSamplesAapClient.persistSampleResource(sample).getContent();
            }

            log.info("Sample " + sample.getName() + " created with accession " + sample.getAccession());

            return sample;
        }

        return null;
    }

    public GridFSDBFile getUploadedFile(String submissionId) throws IllegalStateException {
        return gridFsTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(submissionId)).limit(1));
    }
}
