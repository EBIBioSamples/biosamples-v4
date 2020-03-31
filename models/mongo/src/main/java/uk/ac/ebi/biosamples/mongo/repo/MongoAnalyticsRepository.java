package uk.ac.ebi.biosamples.mongo.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import uk.ac.ebi.biosamples.mongo.model.MongoAnalytics;

import java.util.List;

public interface MongoAnalyticsRepository extends MongoRepository<MongoAnalytics, String> {
    @Query("{ '_id' : { $gt: ?0, $lt: ?1 } }")
    List<MongoAnalytics> findMongoAnalyticsByIdBetween(String start, String end);
}
