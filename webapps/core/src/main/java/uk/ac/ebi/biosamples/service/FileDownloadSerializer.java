package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.dom4j.Document;
import uk.ac.ebi.biosamples.model.Sample;

import java.io.IOException;

public interface FileDownloadSerializer {

    static FileDownloadSerializer getSerializerFor(String format) {
        return "xml".equalsIgnoreCase(format) ? new FileDownloadXmlSerializer() : new FileDownloadJsonSerializer();
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

    class FileDownloadXmlSerializer implements FileDownloadSerializer {
        private final SampleToXmlConverter converter = new SampleToXmlConverter(new ExternalReferenceService());


        public String asString(Sample sample) throws IOException {
            return converter.convert(sample).getRootElement().asXML();
        }

        public String startOfFile() {
            return "<BioSamples>" + System.lineSeparator();
        }

        public String endOfFile() {
            return "</BioSamples>";
        }

        public String delimiter() {
            return System.lineSeparator();
        }
    }
}
