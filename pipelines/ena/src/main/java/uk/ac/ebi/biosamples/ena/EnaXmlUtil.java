package uk.ac.ebi.biosamples.ena;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class EnaXmlUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnaXmlUtil.class);

    public static String pretty(String xmlString) {
        SAXReader reader = new SAXReader();
        Document document = null;
        try {
            document = reader.read(new StringReader(xmlString));
        } catch (DocumentException e) {
            LOGGER.error("Error reading XML", e);
        }
        return pretty(document);
    }

    public static String pretty(Document document) {
        OutputFormat format = OutputFormat.createPrettyPrint();
        StringWriter outputWriter = new StringWriter();
        XMLWriter writer = new XMLWriter(outputWriter, format);
        try {
            writer.write(document);
            outputWriter.close();
            writer.close();
        } catch (IOException e) {
            LOGGER.error("Error writing XML", e);
        }
        return outputWriter.toString();
    }
}
