package uk.ac.ebi.biosamples.utils;

import com.google.common.base.Strings;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

public class BioSchemasRdfGenerator {
    private static final String LDJSON = ".ldjson";
    private static final String BIOSAMPLES_BASE_URI = "https://www.ebi.ac.uk/biosamples/samples/";
    private static final String MONGO_DB_CONNECT_STRING = "mongodb://biosd:TkL51qjIt81aKQ@mongodb-hxvm-009.ebi.ac.uk:27017," +
            "mongodb-hhvm-008.ebi.ac.uk:27017/biosamples?replicaSet=biosrs003&readPreference=nearest&authSource=admin&w=majority";
    private static final String BIOSAMPLES = "biosamples";
    private static final String MONGO_SAMPLE = "mongoSample";
    private static final String FILE_PATH = "c:\\users\\dgupta\\samples_bioschemas_2_turtle.xml";
    private static File file;

    static {
        file = new File(FILE_PATH);
    }

    public static void main(String[] args) {
        final MongoClientURI uri = new MongoClientURI(MONGO_DB_CONNECT_STRING);
        final MongoClient mongoClient = new MongoClient(uri);
        final MongoDatabase db = mongoClient.getDatabase(BIOSAMPLES);
        final MongoCollection<Document> coll = db.getCollection(MONGO_SAMPLE);

        try {
            final List<String> listOfAccessions = getAllDocuments(coll);
            mongoClient.close();

            listOfAccessions.forEach(accession -> {
                if (!Strings.isNullOrEmpty(accession)) {
                    try {
                        URL url = new URL(BIOSAMPLES_BASE_URI + accession + LDJSON);
                        requestHTTPAndHandle(url);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mongoClient.close();
        }
    }

    @SuppressWarnings(value = "deprecation")
    private static void write(final String sampleData) throws Exception {
        FileUtils.writeStringToFile(file, sampleData, true);
    }

    private static List<String> getAllDocuments(final MongoCollection<Document> col) throws Exception {
        final List<String> listOfAccessions = new ArrayList<>();

        col.find().forEach((Consumer<? super Document>) doc -> {
            listOfAccessions.add(doc.getString("_id"));
        });

        return listOfAccessions;
    }

    /*@SuppressWarnings(value = "deprecation")
	private static void queryRDFPlatform(final String url) throws Exception {
        final HttpClient httpclient = HttpClients.createDefault();
        final String uri = RDF_PLATFORM_URI + SOURCE + "/" + TARGET + "/" + url;
        final HttpPost httpPost = new HttpPost(uri);
        final HttpResponse response = httpclient.execute(httpPost);

        if (response.getStatusLine().getStatusCode() == 200) {
            final HttpEntity entity = response.getEntity();

            if (entity != null) write(IOUtils.toString(entity.getContent()));
        }
    }*/

    /*private static void accept(Document doc) {
        final List<String> listOfAccessions = new ArrayList<>();
        try {
            final String id = doc.getString("_id");

            if (!Strings.isNullOrEmpty(id)) queryRDFPlatform(BIOSAMPLES_BASE_URI + id + LDJSON);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    /**
     * @param in a rdf input stream
     * @return a string representation
     */
    private static String readRdfToString(final InputStream in) {
        return graphToString(readRdfToGraph(in));
    }

    /**
     * @param inputStream an Input stream containing rdf data
     * @return a Graph representing the rdf in the input stream
     */
    private static Collection<Statement> readRdfToGraph(final InputStream inputStream) {
        try {
            final RDFParser rdfParser = Rio.createParser(RDFFormat.JSONLD);
            final StatementCollector collector = new StatementCollector();

            rdfParser.setRDFHandler(collector);
            rdfParser.parse(inputStream, "");

            return collector.getStatements();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Transforms a graph to a string.
     *
     * @param myGraph a sesame rdf graph
     * @return a rdf string
     */
    private static String graphToString(final Collection<Statement> myGraph) {
        final StringWriter out = new StringWriter();
        final RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, out);

        return getString(myGraph, out, writer);
    }

    private static String getString(Collection<Statement> myGraph, StringWriter out, RDFWriter writer) {
        return rdfCompose(myGraph, out, writer);
    }

    static String rdfCompose(Collection<Statement> myGraph, StringWriter out, RDFWriter writer) {
        try {
            writer.startRDF();

            for (Statement st : myGraph) {
                writer.handleStatement(st);
            }

            writer.endRDF();
        } catch (RDFHandlerException e) {
            throw new RuntimeException(e);
        }

        return out.getBuffer().toString();
    }


    private static void requestHTTPAndHandle(final URL url) throws Exception {
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        int response = 0;

        try {
            conn.setRequestMethod("GET");
            conn.connect();
            response = conn.getResponseCode();

            if (response == 200) {
                handleSuccessResponses(url);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            conn.disconnect();
        }
    }

    private static void handleSuccessResponses(final URL url) throws IOException, Exception {
        try (Scanner sc = new Scanner(url.openStream())) {
            final StringBuilder sb = new StringBuilder();
            while (sc.hasNext()) {
                sb.append(sc.nextLine());
            }

            try (InputStream in = new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8))) {
                String dataAsRdf = readRdfToString(in);
                write(dataAsRdf);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}

