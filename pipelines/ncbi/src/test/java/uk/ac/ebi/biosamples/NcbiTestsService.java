package uk.ac.ebi.biosamples;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class NcbiTestsService {

    public static Element readNcbiBiosampleElementFromFile(String pathToFile)  {
        try {
            InputStream xmlInputStream = NcbiTestsService.class.getResourceAsStream(pathToFile);
            String xmlDocument = new BufferedReader(new InputStreamReader(xmlInputStream)).lines().collect(Collectors.joining());
            Document doc = DocumentHelper.parseText(xmlDocument);
            return doc.getRootElement().element("BioSample");
        } catch (DocumentException exp) {
            throw new RuntimeException(exp);
        }
    }

}
