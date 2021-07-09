package uk.ac.ebi.biosamples.service.upload;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSFile;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

@Service
public class FileQueueService {
    private static final Logger log = LoggerFactory.getLogger(FileQueueService.class);

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    MessagingService messagingService;

    @Autowired
    MongoFileUploadRepository mongoFileUploadRepository;

    public String queueFile(final MultipartFile file, final String aapDomain, final String checklist, final String webinId) {
        log.info("File queued");

        try {
            final String fileId = persistUploadedFile(file);
            final boolean isWebin = webinId != null && !webinId.isEmpty();

            if (fileId != null) {
                final MongoFileUpload mongoFileUpload = new MongoFileUpload(fileId, BioSamplesFileUploadSubmissionStatus.ACTIVE, isWebin ? webinId : aapDomain, checklist, isWebin, new ArrayList<>());

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

    public String persistUploadedFile(final MultipartFile file) throws IOException {
        final DBObject metaData = new BasicDBObject();

        metaData.put("upload_timestamp", new Date());

        GridFSFile gridFSFile =
                gridFsTemplate.store(
                        file.getInputStream(), file.getOriginalFilename(), file.getContentType(), metaData);

        return gridFSFile.getId().toString();
    }
}
