package uk.ac.ebi.biosamples.utils.bioschemasrestclient;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
public class BioSchemasRestClientRunner implements ApplicationRunner {
    private static final String LDJSON = ".ldjson";
    private static final String BIOSAMPLES_BASE_URI = "https://www.ebi.ac.uk/biosamples/samples/";
    private static final String MONGO_DB_CONNECT_STRING = "mongodb://biosd:TkL51qjIt81aKQ@mongodb-hxvm-009.ebi.ac.uk:27017," +
            "mongodb-hhvm-008.ebi.ac.uk:27017/biosamples?replicaSet=biosrs003&readPreference=nearest&authSource=admin&w=majority";
    private static final String BIOSAMPLES = "biosamples";
    private static final String MONGO_SAMPLE = "mongoSample";
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        final MongoClientURI uri = new MongoClientURI(MONGO_DB_CONNECT_STRING);
        final MongoClient mongoClient = new MongoClient(uri);
        final MongoDatabase db = mongoClient.getDatabase(BIOSAMPLES);
        final MongoCollection<Document> coll = db.getCollection(MONGO_SAMPLE);

        final List<String> listOfAccessions = getAllDocuments(coll);
        System.out.println("List size is : " + listOfAccessions.size());
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

    private static List<String> getAllDocuments(final MongoCollection<Document> col) throws Exception {
        final List<String> listOfAccessions = new ArrayList<>();

        col.find(Filters.eq("_id", "SAMEA6032091")).forEach((Consumer<? super Document>) doc -> {
            listOfAccessions.add(doc.getString("_id"));
        });

        return listOfAccessions;
    }
}
