package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import uk.ac.ebi.biosamples.model.Sample;

import java.io.IOException;

public interface FileDownloadSerializer {

    static FileDownloadSerializer getSerializerFor(String format) {
        return "xml".equals(format) ? new FileDownloadXmlSerializer() : new FileDownloadJsonSerializer();
    }

    String asString(Sample sample) throws IOException;

    String startOfFile();

    String endOfFile();

    String delimiter();

    class FileDownloadJsonSerializer implements FileDownloadSerializer {
        private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        public String asString(Sample sample) throws IOException {
            return objectMapper.writeValueAsString(sample);
        }

        public String startOfFile() {
            return "[";
        }

        public String endOfFile() {
            return "]";
        }

        public String delimiter() {
            return "," + System.lineSeparator();
        }

    }

    //todo this xml serialization is different from individual sample serialization, converge two.
    class FileDownloadXmlSerializer implements FileDownloadSerializer {
        private final ObjectMapper objectMapper = new XmlMapper().enable(SerializationFeature.INDENT_OUTPUT);

        public String asString(Sample sample) throws IOException {
            return objectMapper.writeValueAsString(sample);
        }

        public String startOfFile() {
            return "<samples>" + System.lineSeparator();
        }

        public String endOfFile() {
            return "</samples>";
        }

        public String delimiter() {
            return "";
        }
    }
}
