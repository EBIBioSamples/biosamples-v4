package uk.ac.ebi.biosamples.rdfgenerator;

import com.mongodb.*;
import com.mongodb.operation.OrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class BioSchemasRestClientRunner implements ApplicationRunner {
    private Logger log = LoggerFactory.getLogger(getClass());
    private static final String LDJSON = ".ldjson";
    private static final String BIOSAMPLES_BASE_URI = "https://www.ebi.ac.uk/biosamples/samples/";
    private static final String BIOSAMPLES = "biosamples";
    private static final String MONGO_SAMPLE = "mongoSample";
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @SuppressWarnings("deprecation")
	@Override
    public void run(ApplicationArguments args) {
        final MongoClientURI uri = new MongoClientURI(mongoUri);
        final MongoClient mongoClient = new MongoClient(uri);
        final DB db = mongoClient.getDB(BIOSAMPLES);
        final DBCollection coll = db.getCollection(MONGO_SAMPLE);

        if(args.getOptionNames().contains("filePath")) {
            String filePath = args.getOptionValues("filePath").stream().findFirst().get();
            BioSchemasRdfGenerator.setFilePath(filePath);
        }

        final List<String> listOfAccessions = getAllDocuments(coll);
        log.info("Total number of samples to be dumped is : " + listOfAccessions.size());
        mongoClient.close();

        try (final AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true,
                1, 10)) {
            listOfAccessions.forEach(accession -> {
                try {
                    final URL url = fetchUrlFromAccession(accession);

                    executorService.submit(new BioSchemasRdfGenerator(url));
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (final Exception e) {
            log.error("Something has broken", e);
            log.error(e.getMessage());
        }
    }

    private URL fetchUrlFromAccession(final String accession) throws MalformedURLException {
        return new URL(BIOSAMPLES_BASE_URI + accession + LDJSON);
    }

    private static List<String> getAllDocuments(final DBCollection col) {
        final List<String> listOfAccessions = new ArrayList<>();
        final DBObject query = QueryBuilder.start().put("release").lessThanEquals(new Date()).get();
        final DBCursor cursor = col.find(query).sort(new BasicDBObject("release", OrderBy.ASC.getIntRepresentation()));

        cursor.forEach(elem -> listOfAccessions.add(elem.get("_id").toString()));

        return listOfAccessions;
    }
}
