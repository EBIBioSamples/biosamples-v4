package uk.ac.ebi.biosamples.mongo.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.biosamples.mongo.model.MongoCurationRule;

public interface MongoCurationRuleRepository extends MongoRepository<MongoCurationRule, String> {
    MongoCurationRule findByAttributePre(String attributePre);

}
