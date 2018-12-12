package uk.ac.ebi.biosamples.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

public class TestUtilities {

    public static String readFileAsString(String pathToFile){

        String fileContent;

        try {
            fileContent = StreamUtils.copyToString(new ClassPathResource(pathToFile).getInputStream(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException("An error occurred while reading resource " + pathToFile, e);
        }

        return fileContent;

    }

    public static JsonNode readJsonDocument(String pathToFile) {
        ObjectMapper mapper = new ObjectMapper();

        String fileContent = TestUtilities.readFileAsString(pathToFile);

        try {
            return mapper.readTree(fileContent);
        } catch (IOException e) {
            throw new RuntimeException("An error occurred while converting the string " + fileContent + " to a JSON object", e);
        }
    }

    public static Document readXmlDocument(String pathToFile)  {
        String fileContent = TestUtilities.readFileAsString(pathToFile);
        try {
            return DocumentHelper.parseText(fileContent);
        } catch (DocumentException exp) {
            throw new RuntimeException(exp);
        }
    }
}
