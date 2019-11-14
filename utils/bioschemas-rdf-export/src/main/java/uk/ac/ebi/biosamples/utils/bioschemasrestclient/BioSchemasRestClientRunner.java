package uk.ac.ebi.biosamples.utils.bioschemasrestclient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.QueryBuilder;

import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;

@Component
public class BioSchemasRestClientRunner implements ApplicationRunner {
    private Logger log = LoggerFactory.getLogger(getClass());
    private static final String LDJSON = ".ldjson";
    private static final String BIOSAMPLES_BASE_URI = "https://www.ebi.ac.uk/biosamples/samples/";
    private static final String BIOSAMPLES = "biosamples";
    private static final String MONGO_SAMPLE = "mongoSample";
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

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

        try (AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true,
                1, 10)) {
            listOfAccessions.forEach(accession -> {
                try {
                    URL url = fetchUrlFromAccession(accession);
                    executorService.submit(new BioSchemasRdfGenerator(url));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private URL fetchUrlFromAccession(String accession) throws MalformedURLException {
        return new URL(BIOSAMPLES_BASE_URI + accession + LDJSON);
    }

    private static List<String> getAllDocuments(final DBCollection col) {
        final List<String> listOfAccessions = new ArrayList<>();
        final DBObject query = QueryBuilder.start().put("release").lessThanEquals(new Date()).get();
        final DBCursor cursor = col.find(query);

        cursor.forEach(elem -> listOfAccessions.add(elem.get("_id").toString()));

        return listOfAccessions;
    }
}
