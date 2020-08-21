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
    private static final Map<String, String> CURATIONS = new HashMap<>();

    public SortedSet<String> getPopularAttributes() {
        return POPULAR_ATTRIBUTES;
    }

    public Set<String> getAbbreviations() {
        return ABBREVIATIONS;
    }

    public Map<String, String> getCurations() {
        return CURATIONS;
    }

    public void loadDataFromClassPathResource() {
        loadPopularAttributes("attributes.csv");
        loadAbbreviations("abbreviations.csv");
        loadCurations("curations.csv");
    }

    private void loadPopularAttributes(String filePath) {
        try {
            Reader in = new BufferedReader(new InputStreamReader(new ClassPathResource(filePath).getInputStream()));
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader("ATTRIBUTE", "COUNT").parse(in);
            for (CSVRecord record : records) {
                String attribute = record.get("ATTRIBUTE");
                POPULAR_ATTRIBUTES.add(attribute);
            }
        } catch (IOException e) {
            LOG.error("Failed to load CSV file at: " + filePath, e);
        }
    }

    public void loadAbbreviations(String filePath) {
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
    }

    public void loadCurations(String filePath) {
        try {
            Reader in = new BufferedReader(new InputStreamReader(new ClassPathResource(filePath).getInputStream()));
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader("ATTRIBUTE", "CURATION").parse(in);
            for (CSVRecord record : records) {
                String attribute = record.get("ATTRIBUTE");
                String curation = record.get("CURATION");
                CURATIONS.put(attribute, curation);
            }
        } catch (IOException e) {
            LOG.error("Failed to load CSV file at: " + filePath, e);
        }
    }
}
