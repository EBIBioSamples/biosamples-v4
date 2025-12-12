package uk.ac.ebi.biosamples.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.biosamples.model.PipelineLastRun;
import uk.ac.ebi.biosamples.model.PipelineName;

import java.util.Optional;

public interface PipelineLastRunRepository extends MongoRepository<PipelineLastRun, String> {
  Optional<PipelineLastRun> findFirstByPipelineName(PipelineName pipelineName);
}
