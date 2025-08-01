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
import java.io.OutputStream;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.core.service.FileDownloadSerializer;

@Service
public class FileDownloadService {
  private final SamplePageService samplePageService;

  public FileDownloadService(final SamplePageService samplePageService) {
    this.samplePageService = samplePageService;
  }

  public InputStream getDownloadStream(
      final String text, final Collection<Filter> filters, final String format, final int count) {
    final FileDownloadSerializer serializer = FileDownloadSerializer.getSerializerFor(format);
    return new FileDownloadInputStream(samplePageService, text, filters, count, serializer);
  }

  public void copyAndCompress(
      final InputStream in, final OutputStream out, final boolean zip, final String format)
      throws IOException {
    if (zip) {
      zip(in, out, format);
    } else {
      StreamUtils.copy(in, out);
    }
  }

  private void zip(final InputStream in, final OutputStream out, final String format)
      throws IOException {
    try (final ZipOutputStream zippedOut = new ZipOutputStream(out)) {
      final ZipEntry zipEntry = new ZipEntry("samples." + format);
      zippedOut.putNextEntry(zipEntry);
      StreamUtils.copy(in, zippedOut);

      zippedOut.closeEntry();
      zippedOut.finish();
    }
  }
}
