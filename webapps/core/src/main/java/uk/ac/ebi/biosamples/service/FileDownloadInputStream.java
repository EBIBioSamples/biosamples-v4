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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import org.apache.commons.io.IOUtils;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.core.service.FileDownloadSerializer;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;

public class FileDownloadInputStream extends InputStream {
  private static final int MAX_DOWNLOAD_SIZE = 100000;
  private static final int PAGE_SIZE = 1000;
  private static final String CURSOR_EOF = "EoF";

  private final SamplePageService samplePageService;
  private final String text;
  private final Collection<Filter> filters;
  private final FileDownloadSerializer serializer;
  private final Queue<Sample> sampleQueue;
  private InputStream sampleStream;
  private String cursor;
  private int sampleCount;
  private final int totalCount;

  FileDownloadInputStream(
      final SamplePageService samplePageService,
      final String text,
      final Collection<Filter> filters,
      final int totalCount,
      final FileDownloadSerializer serializer) {
    this.samplePageService = samplePageService;
    this.text = text;
    this.filters = filters;
    this.serializer = serializer;

    this.totalCount = Math.min(MAX_DOWNLOAD_SIZE, totalCount);
    sampleQueue = new LinkedList<>();
    cursor = "*";
    sampleCount = 0;
  }

  @Override
  public int read() throws IOException {
    if (sampleStream == null) {
      sampleStream = generateStream(serializer.startOfFile());
    }

    int nextByte = sampleStream.read();
    if (nextByte == -1 && !CURSOR_EOF.equals(cursor)) {
      sampleStream = generateStream(serializer.delimiter());
      nextByte = sampleStream.read();
    }

    return nextByte;
  }

  private InputStream generateStream(final String delimiter) throws IOException {
    final InputStream inputStream;
    if (sampleQueue.isEmpty() && !CURSOR_EOF.equals(cursor)) {
      loadSamples();
    }

    if (!CURSOR_EOF.equals(cursor)) {
      final Sample sample = sampleQueue.poll();
      sampleCount++;
      inputStream = toInputStream(delimiter, serializer.asString(sample));
      if (sampleCount >= totalCount) {
        cursor = CURSOR_EOF;
      }
    } else if (delimiter.equalsIgnoreCase(serializer.startOfFile())) {
      inputStream =
          toInputStream(
              serializer.startOfFile() + serializer.endOfFile(), ""); // empty search results
    } else {
      inputStream = toInputStream(serializer.endOfFile(), "");
    }

    return inputStream;
  }

  private void loadSamples() {
    final CursorArrayList<Sample> samplePage =
        samplePageService.getSamplesByText(text, filters, null, cursor, PAGE_SIZE, true);
    if (!samplePage.isEmpty()) {
      sampleQueue.addAll(samplePage);
      cursor = samplePage.getNextCursorMark();
    } else {
      cursor = CURSOR_EOF; // mark end of samples
    }
  }

  private InputStream toInputStream(final String delimiter, final String sample) {
    return IOUtils.toInputStream(delimiter + sample, StandardCharsets.UTF_8);
  }
}
