/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.util.Objects;
import uk.ac.ebi.biosamples.model.Sample;

public interface FileDownloadSerializer {

  static FileDownloadSerializer getSerializerFor(String format) {
    FileDownloadSerializer serializer;
    if ("txt".equalsIgnoreCase(format)) {
      serializer = new FileDownloadAccessionsSerializer();
    } else if ("xml".equalsIgnoreCase(format)) {
      serializer = new FileDownloadXmlSerializer();
    } else {
      serializer = new FileDownloadJsonSerializer();
    }
    return serializer;
  }

  String asString(Sample sample) throws IOException;

  String startOfFile();

  String endOfFile();

  String delimiter();

  class FileDownloadJsonSerializer implements FileDownloadSerializer {
    private final ObjectMapper objectMapper =
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

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
    private final SampleToXmlConverter converter =
        new SampleToXmlConverter(new ExternalReferenceService());

    public String asString(Sample sample) {
      return Objects.requireNonNull(converter.convert(sample)).getRootElement().asXML();
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

  class FileDownloadAccessionsSerializer implements FileDownloadSerializer {
    public String asString(Sample sample) {
      return sample.getAccession();
    }

    public String startOfFile() {
      return "";
    }

    public String endOfFile() {
      return "";
    }

    public String delimiter() {
      return System.lineSeparator();
    }
  }
}
