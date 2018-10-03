package uk.ac.ebi.biosamples.ncbi;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class TestUtilities {

    public Element readNcbiBiosampleSetElementFromFile(String filePath) throws DocumentException, IOException {
        Resource resource = new ClassPathResource(filePath);
        InputStream resourceStream = resource.getInputStream();

        SAXReader reader = new SAXReader();
        return reader.read(resourceStream).getRootElement();
    }

    public String readNcbiBiosampleSetStringFromFile(String filePath) throws IOException {
        Resource resource = new ClassPathResource(filePath);
        BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()));

        return br.lines().collect(Collectors.joining());
    }



}
