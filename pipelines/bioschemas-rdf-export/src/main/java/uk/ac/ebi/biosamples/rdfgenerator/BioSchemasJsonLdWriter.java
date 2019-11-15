package uk.ac.ebi.biosamples.rdfgenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

import org.bson.Document;

import com.google.common.base.Strings;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class BioSchemasJsonLdWriter {
    private static final String LDJSON = ".ldjson";
    private static final String BIOSAMPLES_BASE_URI = "https://www.ebi.ac.uk/biosamples/samples/";
    private static final String BIOSAMPLES = "biosamples";
    private static final String MONGO_SAMPLE = "mongoSample";
    private static final String MONGO_DB_CONNECT_STRING = "";
    static File file;

    static {
        file = new File("");
    }

    public static void main(String[] args) throws IOException {
        final String db_name = BIOSAMPLES, db_coll_name = MONGO_SAMPLE;
        final String client_url = MONGO_DB_CONNECT_STRING;
        final MongoClientURI uri = new MongoClientURI(client_url);
        final MongoClient mongoClient = new MongoClient(uri);
        final MongoDatabase db = mongoClient.getDatabase(db_name);
        final MongoCollection<Document> coll = db.getCollection(db_coll_name);

        try {
            getAllDocuments(coll);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mongoClient.close();
        }
    }

    private static void requestHTTPAndHandle(final String inline, final URL url) throws Exception {
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        int responsecode = 0;

        try {
            conn.setRequestMethod("GET");
            conn.connect();
            responsecode = conn.getResponseCode();

            if (responsecode == 200) {
                handleSuccessResponses(inline, url);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("URI Not found");
        } finally {
            conn.disconnect();
        }
    }

    private static void handleSuccessResponses(String inline, final URL url) throws IOException, Exception {
        final Scanner sc = new Scanner(url.openStream());

        try {
            while (sc.hasNext()) {
                write(sc.nextLine(), true);
            }
        } finally {
            sc.close();
        }
    }

    private static void write(final String sampleData, final Boolean append) throws Exception {
        try (final FileWriter writer = new FileWriter(file.getAbsoluteFile(), append)) {
            writer.append(sampleData);
        }
    }

    private static void getAllDocuments(MongoCollection<Document> col) throws Exception {
        final List<String> sampleAccessionList = new ArrayList<>();
        final String inline = "";

        /*for (Document doc : fi) {
            final String id = doc.getString("_id");
            sampleAccessionList.add(id);
            System.out.println("List size: " + sampleAccessionList.size());
        }*/

        col.find().forEach((Consumer<? super Document>) doc -> {
            final String id = doc.getString("_id");
            sampleAccessionList.add(id);
            System.out.println("List size: " + sampleAccessionList.size());
        });

        sampleAccessionList.forEach(id -> {
            if (!Strings.isNullOrEmpty(id)) {
                URL url = null;

                try {
                    url = new URL(BIOSAMPLES_BASE_URI + id + LDJSON);
                    requestHTTPAndHandle(inline, url);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}

