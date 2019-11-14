package uk.ac.ebi.biosamples.utils.bioschemasrestclient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;

@Component
public class BioSchemasRestClientRunner implements ApplicationRunner {
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
        final MongoDatabase db = mongoClient.getDatabase(BIOSAMPLES);
        final MongoCollection<Document> coll = db.getCollection(MONGO_SAMPLE);

        if(args.getOptionNames().contains("filePath")) {
            String filePath = args.getOptionValues("filePath").stream().findFirst().get();
            BioSchemasRdfGenerator.setFilePath(filePath);
        }

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

    private static List<String> getAllDocuments(final MongoCollection<Document> col) {
        final List<String> listOfAccessions = new ArrayList<>();

        col.find().forEach((Consumer<? super Document>) doc -> listOfAccessions.add(doc.getString("_id")));

        return listOfAccessions;
    }
}
