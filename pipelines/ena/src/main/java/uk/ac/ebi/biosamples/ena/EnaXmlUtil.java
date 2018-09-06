package uk.ac.ebi.biosamples.ena;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class EnaXmlUtil {

    public static String pretty(String xmlString) throws IOException, DocumentException {
        SAXReader reader = new SAXReader();
        Document document = reader.read(new StringReader(xmlString));
        return pretty(document);
    }

    public static String pretty(Document document) throws IOException {
        OutputFormat format = OutputFormat.createPrettyPrint();
        StringWriter outputWriter = new StringWriter();
        XMLWriter writer = new XMLWriter(outputWriter, format);
        writer.write(document);
        outputWriter.close();
        writer.close();
        return outputWriter.toString();
    }
}
