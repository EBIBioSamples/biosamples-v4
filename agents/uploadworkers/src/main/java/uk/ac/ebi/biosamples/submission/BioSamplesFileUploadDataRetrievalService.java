package uk.ac.ebi.biosamples.submission;

import com.mongodb.gridfs.GridFSDBFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.BioSamplesSubmissionFile;

import java.io.InputStream;

@Service
public class BioSamplesFileUploadDataRetrievalService {
    @Autowired
    private GridFsTemplate gridFsTemplate;

    public BioSamplesSubmissionFile getFile(final String submissionId)
            throws IllegalStateException {
        final GridFSDBFile file =
                gridFsTemplate.findOne(
                        new Query()
                                .addCriteria(Criteria.where("_id").is(submissionId))
                                .limit(1));

        if (file != null) {
            final InputStream fileStream = file.getInputStream();
            final BioSamplesSubmissionFile bioSamplesSubmissionFile = new BioSamplesSubmissionFile(file.getFilename(), fileStream);

            return bioSamplesSubmissionFile;
        }

        return null;
    }
}
