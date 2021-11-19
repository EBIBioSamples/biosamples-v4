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
package uk.ac.ebi.biosamples.service.upload;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.biosamples.mongo.model.MongoFileUpload;
import uk.ac.ebi.biosamples.mongo.repo.MongoFileUploadRepository;
import uk.ac.ebi.biosamples.mongo.util.BioSamplesFileUploadSubmissionStatus;
import uk.ac.ebi.biosamples.service.MessagingService;

@Service
public class FileQueueService {
  private static final Logger log = LoggerFactory.getLogger(FileQueueService.class);

  @Autowired private GridFsTemplate gridFsTemplate;
  @Autowired private MessagingService messagingService;
  @Autowired private MongoFileUploadRepository mongoFileUploadRepository;

  public String queueFileinMongoAndSendMessageToRabbitMq(
      final MultipartFile file,
      final String aapDomain,
      final String checklist,
      final String webinId) {
    try {
      final String fileId = persistUploadedFileInMongo(file);
      final boolean isWebin = webinId != null && !webinId.isEmpty();

      if (fileId != null) {
        final MongoFileUpload mongoFileUpload =
            new MongoFileUpload(
                fileId,
                BioSamplesFileUploadSubmissionStatus.ACTIVE,
                isWebin ? webinId : aapDomain,
                checklist,
                isWebin,
                new ArrayList<>(),
                null);

        mongoFileUploadRepository.insert(mongoFileUpload);
        messagingService.sendFileUploadedMessage(fileId);

        return fileId;
      } else {
        throw new RuntimeException("Failed to save Submission");
      }
    } catch (final Exception e) {
      final String message = "Failed to save Submission";

      log.error(message, e);
      throw new RuntimeException(message, e);
    }
  }

  public String persistUploadedFileInMongo(final MultipartFile file) throws IOException {
    final DBObject metaData = new BasicDBObject();

    metaData.put("upload_timestamp", new Date());

    GridFSFile gridFSFile =
        gridFsTemplate.store(
            file.getInputStream(), file.getOriginalFilename(), file.getContentType(), metaData);

    return gridFSFile.getId().toString();
  }
}
