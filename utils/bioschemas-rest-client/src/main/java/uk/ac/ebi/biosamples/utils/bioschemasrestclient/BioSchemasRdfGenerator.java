package uk.ac.ebi.biosamples.utils.bioschemasrestclient;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Scanner;
import java.util.concurrent.Callable;

public class BioSchemasRdfGenerator implements Callable<Void> {
    private static final String FILE_PATH = "c:\\users\\dgupta\\file_4.ttl";
    private static File file;
    private static int sampleCount = 0;
    private final URL url;

    static {
        file = new File(FILE_PATH);
    }

    BioSchemasRdfGenerator(final URL url) {
        System.out.println("HANDLING " + url.toString() + " sample count: " + ++sampleCount);
        this.url = url;
    }

    @Override
    public Void call() throws Exception {
        requestHTTPAndHandle(this.url);
        return null;
    }

    @SuppressWarnings(value = "deprecation")
    private static void write(final String sampleData) throws Exception {
        FileUtils.writeStringToFile(file, sampleData, true);
    }

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

        return writeRdfTurtle(myGraph, out, writer);
    }

    private static String writeRdfTurtle(Collection<Statement> myGraph, StringWriter out, RDFWriter writer) {
        try {
            writer.startRDF();
            handleNamespaces(writer);

            for (Statement st : myGraph) {
                writer.handleStatement(st);
            }

            writer.endRDF();
        } catch (RDFHandlerException e) {
            throw new RuntimeException(e);
        }

        return out.getBuffer().toString();
    }

    private static void handleNamespaces(RDFWriter writer) {
        writer.handleNamespace("NAME", "http://schema.org/Sample");
        writer.handleNamespace("PURL", "http://purl.obolibrary.org/");
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

