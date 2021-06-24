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
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.utils.upload.Characteristics;
import uk.ac.ebi.biosamples.utils.upload.FileUploadIsaTabUtils;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FileUploadMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());
    private ValidationResult validationResult;
    private FileUploadIsaTabUtils fileUploadIsaTabUtils;

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
            fileUploadIsaTabUtils = new FileUploadIsaTabUtils();

            log.info(
                    "File with mongo file id "
                            + mongoFileId);

            final GridFSDBFile file = getUploadedFile(mongoFileId);

            // get file metadata, determine webin aur aap auth and use client accordingly
            String aapDomain = file.getMetaData().toMap().get("aap_domain").toString();
            String webinId = file.getMetaData().toMap().get("webin_id").toString();
            String checklist = file.getMetaData().toMap().get("certificate").toString();

            final Path temp = Files.createTempFile("upload", ".tsv");

            Files.copy(
                    file.getInputStream(),
                    temp,
                    StandardCopyOption.REPLACE_EXISTING);

            File fileToBeUploaded = temp.toFile();
            FileReader fr = new FileReader(fileToBeUploaded);
            BufferedReader reader = new BufferedReader(fr);

            final CSVParser csvParser = fileUploadIsaTabUtils.buildParser(reader);
            final List<Multimap<String, String>> csvDataMap = fileUploadIsaTabUtils.getCSVDataInMap(csvParser);

            log.info("CSV data size: " + csvDataMap.size());

            final List<Sample> samples = buildSamples(csvDataMap, aapDomain, webinId, checklist, validationResult);

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
            ValidationResult validationResult) {
        final Map<String, String> sampleNameToAccessionMap = new LinkedHashMap<>();
        final Map<Sample, Multimap<String, String>> sampleToMappedSample = new LinkedHashMap<>();

        csvDataMap.forEach(
                csvRecordMap -> {
                    Sample sample = null;

                    try {
                        sample =
                                buildSample(
                                        csvRecordMap, aapDomain, webinId, checklist);

                        if (sample == null) {
                            this.validationResult.addValidationMessage("Failed to create all samples in the file");
                        }
                    } catch (Exception e) {
                        this.validationResult.addValidationMessage("Failed to create all samples in the file");
                    }

                    if (sample != null) {
                        sampleNameToAccessionMap.put(sample.getName(), sample.getAccession());
                        sampleToMappedSample.put(sample, csvRecordMap);
                    }
                });

        return addRelationshipsAndThenBuildSamples(
                sampleNameToAccessionMap, sampleToMappedSample, validationResult);
    }

    private boolean isWebinIdUsedToAuthenticate(String webinId) {
        return webinId != null && webinId.toUpperCase().startsWith("WEBIN");
    }

    private List<Sample> addRelationshipsAndThenBuildSamples(
            Map<String, String> sampleNameToAccessionMap,
            Map<Sample, Multimap<String, String>> sampleToMappedSample,
            ValidationResult validationResult) {
        return sampleToMappedSample.entrySet().stream()
                .map(
                        sampleMultimapEntry ->
                                addRelationshipAndThenBuildSample(
                                        sampleNameToAccessionMap, sampleMultimapEntry, validationResult))
                .collect(Collectors.toList());
    }

    private Sample addRelationshipAndThenBuildSample(
            Map<String, String> sampleNameToAccessionMap,
            Map.Entry<Sample, Multimap<String, String>> sampleMultimapEntry,
            ValidationResult validationResult) {
        final Multimap<String, String> relationshipMap =
                fileUploadIsaTabUtils.parseRelationships(sampleMultimapEntry.getValue());
        Sample sample = sampleMultimapEntry.getKey();
        final List<Relationship> relationships =
                fileUploadIsaTabUtils.createRelationships(sample, sampleNameToAccessionMap, relationshipMap, validationResult);

        relationships.forEach(relationship -> log.trace(relationship.toString()));

        sample = Sample.Builder.fromSample(sample).withRelationships(relationships).build();

        final String webinSubmissionAccountId = sample.getWebinSubmissionAccountId();

        if (webinSubmissionAccountId != null && webinSubmissionAccountId.startsWith("WEBIN")) {
            //sample = bioSamplesWebinClient.persistSampleResource(sample).getContent();
        } else {
            //sample = bioSamplesAapClient.persistSampleResource(sample).getContent();
        }

        return sample;
    }

    private Sample buildSample(
            Multimap<String, String> multiMap,
            String aapDomain,
            String webinId,
            String checklist) {
        final String sampleName = fileUploadIsaTabUtils.getSampleName(multiMap);
        final String sampleReleaseDate = fileUploadIsaTabUtils.getReleaseDate(multiMap);
        final String accession = fileUploadIsaTabUtils.getSampleAccession(multiMap);
        final List<Characteristics> characteristicsList = fileUploadIsaTabUtils.handleCharacteristics(multiMap);
        final List<ExternalReference> externalReferenceList = fileUploadIsaTabUtils.handleExternalReferences(multiMap);
        final List<Contact> contactsList = fileUploadIsaTabUtils.handleContacts(multiMap);
        boolean basicValidationFailure = false;

        if (sampleName == null || sampleName.isEmpty()) {
            validationResult.addValidationMessage(
                    "All samples in the file must have a sample name, some samples are missing sample name and hence are not created");
            basicValidationFailure = true;
        }

        if (sampleReleaseDate == null || sampleReleaseDate.isEmpty()) {
            validationResult.addValidationMessage(
                    "All samples in the file must have a release date "
                            + sampleName
                            + " doesn't have a release date and is not created");
            basicValidationFailure = true;
        }

        if (!basicValidationFailure) {
            Sample sample = fileUploadIsaTabUtils.buildSample(sampleName, accession, characteristicsList, externalReferenceList, contactsList);

            final Set<Attribute> attributeSet = sample.getAttributes();
            final Attribute attribute =
                    new Attribute.Builder("checklist", checklist)
                            .build();

            attributeSet.add(attribute);
            sample = Sample.Builder.fromSample(sample).withAttributes(attributeSet).build();

            if (isWebinIdUsedToAuthenticate(webinId)) {
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
