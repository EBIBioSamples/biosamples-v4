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

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;

@RunWith(MockitoJUnitRunner.class)
public class FileDownloadInputStreamTest {
  @Mock private SamplePageService samplePageService;

  private FileDownloadInputStream fileDownloadInputStream;

  private final int pageSize = 1000;
  private final int sampleCount = 100;
  private final String cursor = "*";
  private final String sampleSearchText = "";
  private final String emptySamplesText = "no samples search";
  private final Collection<Filter> filters = Collections.emptyList();
  private final Collection<String> domains = Collections.emptyList();

  @Before
  public void init() {
    final CursorArrayList<Sample> samplePage = new CursorArrayList<>(cursor);
    when(samplePageService.getSamplesByText(
            emptySamplesText, filters, domains, null, cursor, pageSize, Optional.empty()))
        .thenReturn(samplePage);

    final CursorArrayList<Sample> samplePageWithSample = new CursorArrayList<>(cursor);
    samplePageWithSample.add(getTestSample());
    when(samplePageService.getSamplesByText(
            sampleSearchText, filters, domains, null, cursor, pageSize, Optional.empty()))
        .thenReturn(samplePageWithSample);
  }

  @Test
  public void read() throws IOException {
    final FileDownloadSerializer serializer = FileDownloadSerializer.getSerializerFor("json");
    fileDownloadInputStream =
        new FileDownloadInputStream(
            samplePageService, emptySamplesText, filters, sampleCount, domains, serializer);

    final int startByte = fileDownloadInputStream.read();
    assertTrue(startByte > 0);
  }

  @Test
  public void read_empty_json() throws IOException {
    final FileDownloadSerializer serializer = FileDownloadSerializer.getSerializerFor("json");
    fileDownloadInputStream =
        new FileDownloadInputStream(
            samplePageService, emptySamplesText, filters, sampleCount, domains, serializer);

    final StringWriter writer = new StringWriter();
    IOUtils.copy(fileDownloadInputStream, writer, Charset.defaultCharset());
    final String emptyJson = writer.toString();
    assertEquals("[]", emptyJson);
  }

  @Test
  @Ignore
  public void read_empty_xml() throws IOException {
    final FileDownloadSerializer serializer = FileDownloadSerializer.getSerializerFor("xml");
    fileDownloadInputStream =
        new FileDownloadInputStream(
            samplePageService, emptySamplesText, filters, sampleCount, domains, serializer);

    final StringWriter writer = new StringWriter();
    IOUtils.copy(fileDownloadInputStream, writer, Charset.defaultCharset());
    final String emptyJson = writer.toString();
    assertEquals("<BioSamples>\n</BioSamples>", emptyJson);
  }

  @Test
  public void read_json_with_samples() throws IOException {
    final FileDownloadSerializer serializer = FileDownloadSerializer.getSerializerFor("json");
    fileDownloadInputStream =
        new FileDownloadInputStream(
            samplePageService, sampleSearchText, filters, sampleCount, domains, serializer);

    final StringWriter writer = new StringWriter();
    IOUtils.copy(fileDownloadInputStream, writer, Charset.defaultCharset());
    final String sampleJson = writer.toString();
    assertNotEquals("[]", sampleJson);
  }

  private Sample getTestSample() {
    final String name = "FileDownloadInputStreamTest_sample";
    final String accession = "fileDownloadTestAccession";
    final Instant release = Instant.parse("2016-04-01T11:36:57.00Z");
    final SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(Attribute.build("organism", "Homo sapiens"));
    attributes.add(Attribute.build("organism_part", "liver"));

    return new Sample.Builder(name)
        .withAccession(accession)
        .withDomain("self.biosamplesUnitTests")
        .withRelease(release)
        .withAttributes(attributes)
        .build();
  }
}
