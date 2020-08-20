package uk.ac.ebi.biosamples.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

public class DataLoader {
    private static final Logger LOG = LoggerFactory.getLogger(DataLoader.class);

    private static final SortedSet<String> POPULAR_ATTRIBUTES = new TreeSet<>();
    private static final Set<String> ABBREVIATIONS = new HashSet<>();

    public SortedSet<String> getPopularAttributes() {
        return POPULAR_ATTRIBUTES;
    }

    public Set<String> getAbbreviations() {
        return ABBREVIATIONS;
    }

    public SortedSet<String> loadPopularAttributes(String filePath) {
        try {
//            Reader in = new BufferedReader(new InputStreamReader(Thread.currentThread().getClass().getResourceAsStream(filePath)));
//            Reader in = new FileReader(filePath);
            Reader in = new BufferedReader(new InputStreamReader(new ClassPathResource(filePath).getInputStream()));
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader("ATTRIBUTE").parse(in);
            for (CSVRecord record : records) {
                String attribute = record.get("ATTRIBUTE");
                POPULAR_ATTRIBUTES.add(attribute);
            }
        } catch (IOException e) {
            LOG.error("Failed to load CSV file at: " + filePath, e);
        }

        return POPULAR_ATTRIBUTES;
    }

    public Set<String> loadAbbreviations(String filePath) {
        try {
            Reader in = new BufferedReader(new InputStreamReader(new ClassPathResource(filePath).getInputStream()));
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader("ATTRIBUTE").parse(in);
            for (CSVRecord record : records) {
                String attribute = record.get("ATTRIBUTE");
                ABBREVIATIONS.add(attribute);
            }
        } catch (IOException e) {
            LOG.error("Failed to load CSV file at: " + filePath, e);
        }

        return ABBREVIATIONS;
    }

    public static void main(String[] args) {
        DataLoader dataLoader = new DataLoader();
        SortedSet<String> attributes = dataLoader.loadPopularAttributes("attributes.csv");

        System.out.println(attributes.size());
        System.out.println(CuramiUtils.getSimilarityScore("hello", "hewwo"));
    }
}
