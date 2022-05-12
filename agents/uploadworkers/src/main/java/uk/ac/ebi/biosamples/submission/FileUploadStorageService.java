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
package uk.ac.ebi.biosamples.submission;

import com.mongodb.client.gridfs.model.GridFSFile;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.SubmissionFile;

@Service
public class FileUploadStorageService {
  @Autowired private GridFsTemplate gridFsTemplate;
  @Autowired private GridFsOperations operations;

  public SubmissionFile getFile(final String submissionId)
      throws IllegalStateException, IOException {
    final GridFSFile file =
        gridFsTemplate.findOne(
            new Query().addCriteria(Criteria.where("_id").is(submissionId)).limit(1));

    if (file != null) {
      final InputStream fileStream = operations.getResource(file).getInputStream();
      final SubmissionFile submissionFile = new SubmissionFile(file.getFilename(), fileStream);

      return submissionFile;
    }

    return null;
  }

  public void deleteFile(final String submissionId) throws IllegalStateException {
    gridFsTemplate.delete(new Query().addCriteria(Criteria.where("_id").is(submissionId)).limit(1));
  }
}
