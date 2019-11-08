package uk.ac.ebi.biosamples.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;


import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;


public class Test {
    @org.junit.Test
    public void testForYourCode() {
        String data = "{\"@context\":[\"http://schema.org\",{\"OBI\":\"http://purl.obolibrary.org/obo/OBI_\",\"biosample\":\"http://identifiers.org/biosample/\"}],\"@type\":\"DataRecord\",\"@id\":\"biosample:SAMEA100386\",\"identifier\":\"biosample:SAMEA100386\",\"dateCreated\":\"2009-02-07T00:00:00Z\",\"dateModified\":\"2019-07-23T09:42:30.299Z\",\"mainEntity\":{\"@type\":[\"Sample\",\"OBI:0000747\"],\"identifier\":[\"biosample:SAMEA100386\"],\"name\":\"source GSE13294GSM335546\",\"additionalProperty\":[{\"@type\":\"PropertyValue\",\"name\":\"organism\",\"value\":\"Homo sapiens\",\"valueReference\":[{\"@id\":\"http://purl.obolibrary.org/obo/NCBITaxon_9606\",\"@type\":\"DefinedTerm\"}]},{\"@type\":\"PropertyValue\",\"name\":\"sample characteristics\",\"value\":\"MSI\"},{\"@type\":\"PropertyValue\",\"name\":\"sample source name\",\"value\":\"primary colorectal adenocarcinoma\"}],\"sameAs\":\"http://identifiers.org/biosample/SAMEA100386\"},\"isPartOf\":{\"@type\":\"Dataset\",\"@id\":\"https://www.ebi.ac.uk/biosamples/samples\"}}";
        try (InputStream in = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))) {
            String dataAsRdf = readRdfToString(in, RDFFormat.JSONLD, RDFFormat.RDFXML, "");
            System.out.println(dataAsRdf);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param in      a rdf input stream
     * @param inf     the rdf format of the input stream
     * @param outf    the output format
     * @param baseUrl usually the url of the resource
     * @return a string representation
     */
    private static String readRdfToString(InputStream in, RDFFormat inf, RDFFormat outf, String baseUrl) {
        Collection<Statement> myGraph = null;
        myGraph = readRdfToGraph(in, inf, baseUrl);
        return graphToString(myGraph, outf);
    }

    /**
     * @param inputStream an Input stream containing rdf data
     * @param inf         the rdf format
     * @param baseUrl     see sesame docu
     * @return a Graph representing the rdf in the input stream
     */
    private static Collection<Statement> readRdfToGraph(final InputStream inputStream, final RDFFormat inf,
                                                        final String baseUrl) {
        try {
            final RDFParser rdfParser = Rio.createParser(inf);
            final StatementCollector collector = new StatementCollector();
            rdfParser.setRDFHandler(collector);
            rdfParser.parse(inputStream, baseUrl);
            return collector.getStatements();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Transforms a graph to a string.
     *
     * @param myGraph a sesame rdf graph
     * @param outf    the expected output format
     * @return a rdf string
     */
    private static String graphToString(Collection<Statement> myGraph, RDFFormat outf) {
        StringWriter out = new StringWriter();
        RDFWriter writer = Rio.createWriter(outf, out);
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
}