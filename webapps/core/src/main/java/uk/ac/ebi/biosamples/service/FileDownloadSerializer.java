package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import uk.ac.ebi.biosamples.model.Sample;

import java.io.IOException;

public interface FileDownloadSerializer {

    static FileDownloadSerializer getJsonSerializer() {
        return new FileDownloadJsonSerializer();
    }

    static FileDownloadSerializer getXmlSerializer() {
        return new FileDownloadXmlSerializer();
    }

    String asString(Sample sample) throws IOException;

    String getStartOfFileContent();

    String getEndOfFileContent();

    String getDelimiter();

    class FileDownloadJsonSerializer implements FileDownloadSerializer {
        private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        public String asString(Sample sample) throws IOException {
            return objectMapper.writeValueAsString(sample);
        }

        public String getStartOfFileContent() {
            return "[";
        }

        public String getEndOfFileContent() {
            return "]";
        }

        public String getDelimiter() {
            return "," + System.lineSeparator();
        }

    }

    class FileDownloadXmlSerializer implements FileDownloadSerializer {
        private final ObjectMapper objectMapper = new XmlMapper().enable(SerializationFeature.INDENT_OUTPUT);

        public String asString(Sample sample) throws IOException {
            return objectMapper.writeValueAsString(sample);
        }

        public String getStartOfFileContent() {
            return "<samples>";
        }

        public String getEndOfFileContent() {
            return "</samples>";
        }

        public String getDelimiter() {
            return System.lineSeparator();
        }
    }
}
