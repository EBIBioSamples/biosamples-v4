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
import uk.ac.ebi.biosamples.service.MessagingService;

import java.io.IOException;
import java.util.Date;

@Service
public class FileQueueService {
    private static final Logger log = LoggerFactory.getLogger(FileQueueService.class);

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    MessagingService messagingService;

    public void queueFile(MultipartFile file, String aapDomain, String certificate, String webinId) throws IOException {
        log.info("File queued");
        String fileId = persistUploadedFile(file, aapDomain, certificate, webinId);

        messagingService.sendFileUploadedMessage(fileId);
    }

    public String persistUploadedFile(MultipartFile file, String aapDomain, String certificate, String webinId) throws IOException {
        final DBObject metaData = new BasicDBObject();

        metaData.put("file_name", file.getName());
        metaData.put("upload_timestamp", new Date());
        metaData.put("aap_domain", aapDomain);
        metaData.put("webin_id", webinId);
        metaData.put("certificate", certificate);

        GridFSFile gridFSFile =
                gridFsTemplate.store(
                        file.getInputStream(), file.getName(), file.getContentType(), metaData);

        return gridFSFile.getId().toString();
    }
}
