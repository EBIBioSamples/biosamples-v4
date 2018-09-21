package uk.ac.ebi.biosamples.ncbi;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

public class TestUtilities {

    public Element readNcbiBiosampleSetFromFile(String filePath) throws DocumentException, IOException {
        Resource resource = new ClassPathResource(filePath);
        InputStream resourceStream = resource.getInputStream();

        SAXReader reader = new SAXReader();
        return reader.read(resourceStream).getRootElement();
    }



}
