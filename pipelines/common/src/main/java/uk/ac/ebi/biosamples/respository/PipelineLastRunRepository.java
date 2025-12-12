package uk.ac.ebi.biosamples.respository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.biosamples.model.PipelineLastRun;
import uk.ac.ebi.biosamples.model.PipelineName;

import java.util.Optional;

@Repository
public interface PipelineLastRunRepository extends MongoRepository<PipelineLastRun, String> {
  Optional<PipelineLastRun> findFirstByPipelineName(PipelineName pipelineName);
}
