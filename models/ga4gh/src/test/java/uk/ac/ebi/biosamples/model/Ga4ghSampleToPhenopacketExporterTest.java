/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.springframework.core.io.ClassPathResource;
import uk.ac.ebi.biosamples.model.ga4gh.Ga4ghAttributes;
import uk.ac.ebi.biosamples.model.ga4gh.Ga4ghSample;
import uk.ac.ebi.biosamples.service.*;

/**
 * Integration Test of phenopacket exporter. For test performing you need to have access to
 * https://www.ebi.ac.uk/ols/api.
 */
public class Ga4ghSampleToPhenopacketExporterTest {
  final String biosample1Path = "/biosample1.json";
  final String biosample2Path = "/biosamples2.json";
  final String biosample3Path = "/biosample3.json";
  final String phenopacket1Path = "/phenopacket1.json";
  final String phenopacket2Path = "/phenopacket2.json";
  final String phenopacket3Path = "/phenopacket3.json";

  public SampleToGa4ghSampleConverter SampleToGa4ghSampleConverter;

  public Ga4ghSampleToPhenopacketConverter biosampleToPhenopacketExporter;
  public String absolutePath;

  public Ga4ghSampleToPhenopacketExporterTest() {
    SampleToGa4ghSampleConverter =
        new SampleToGa4ghSampleConverter(
            new Ga4ghSample(new Ga4ghAttributes()),
            new GeoLocationDataHelper(),
            new HttpOlsUrlResolutionService());
    biosampleToPhenopacketExporter =
        new Ga4ghSampleToPhenopacketConverter(SampleToGa4ghSampleConverter, new OLSDataRetriever());
  }

  // TODO Uncomment these tests
  /*@Test
  public void exportation_test1() throws IOException, JSONException {

    ObjectMapper mapper = new ObjectMapper();
    Sample sample = mapper.readValue(getJsonString(biosample1Path), Sample.class);
    Ga4ghSample ga4ghSample = SampleToGa4ghSampleConverter.convert(sample);
    String actualJson = biosampleToPhenopacketExporter.getJsonFormattedPhenopacket(ga4ghSample);
    String expectedJson = getJsonString(phenopacket1Path);

    ArrayValueMatcher<Object> arrayValueMatcher =
        new ArrayValueMatcher<>(
            new CustomComparator(
                JSONCompareMode.NON_EXTENSIBLE,
                new Customization("metaData.resources[*].version", (o1, o2) -> true)));

    JSONAssert.assertEquals(
        expectedJson,
        actualJson,
        new CustomComparator(
            JSONCompareMode.LENIENT,
            new Customization("metaData.created", (o1, o2) -> true),
            new Customization("metaData.resources", arrayValueMatcher)));
  }

  @Test
  public void exportation_test2() throws IOException, JSONException {
    ObjectMapper mapper = new ObjectMapper();
    Sample sample = mapper.readValue(getJsonString(biosample2Path), Sample.class);
    Ga4ghSample ga4ghSample = SampleToGa4ghSampleConverter.convert(sample);
    String actualJson = biosampleToPhenopacketExporter.getJsonFormattedPhenopacket(ga4ghSample);
    String expectedJson = getJsonString(phenopacket2Path);

    ArrayValueMatcher<Object> arrayValueMatcher =
        new ArrayValueMatcher<>(
            new CustomComparator(
                JSONCompareMode.NON_EXTENSIBLE,
                new Customization("metaData.resources[*].version", (o1, o2) -> true)));

    JSONAssert.assertEquals(
        expectedJson,
        actualJson,
        new CustomComparator(
            JSONCompareMode.LENIENT,
            new Customization("metaData.created", (o1, o2) -> true),
            new Customization("metaData.resources", arrayValueMatcher)));
  }

  @Test
  public void exportation_test3() throws IOException, JSONException {
    ObjectMapper mapper = new ObjectMapper();
    Sample sample = mapper.readValue(getJsonString(biosample3Path), Sample.class);
    Ga4ghSample ga4ghSample = SampleToGa4ghSampleConverter.convert(sample);
    String actualJson = biosampleToPhenopacketExporter.getJsonFormattedPhenopacket(ga4ghSample);
    String expectedJson = getJsonString(phenopacket3Path);

    ArrayValueMatcher<Object> arrayValueMatcher =
        new ArrayValueMatcher<>(
            new CustomComparator(
                JSONCompareMode.NON_EXTENSIBLE,
                new Customization("metaData.resources[*].version", (o1, o2) -> true)));

    JSONAssert.assertEquals(
        expectedJson,
        actualJson,
        new CustomComparator(
            JSONCompareMode.LENIENT,
            new Customization("metaData.created", (o1, o2) -> true),
            new Customization("metaData.resources", arrayValueMatcher)));
  }*/

  static String getJsonString(String path) throws IOException {
    BufferedReader br =
        new BufferedReader(
            new InputStreamReader(new ClassPathResource(path).getInputStream()), 4096);
    StringBuilder stringBuilder = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      stringBuilder.append(line).append('\n');
    }
    br.close();
    String expectedJson = stringBuilder.toString();
    return expectedJson;
  }
}
