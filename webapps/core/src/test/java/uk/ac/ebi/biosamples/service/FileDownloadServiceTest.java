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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;

@RunWith(MockitoJUnitRunner.class)
public class FileDownloadServiceTest {
  @Mock SamplePageService samplePageService;
  FileDownloadService fileDownloadService;

  @Before
  public void init() {
    fileDownloadService = new FileDownloadService(samplePageService);
  }

  @Test
  public void copyAndCompress() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    InputStream in = new ClassPathResource("amr_sample.json").getInputStream();
    fileDownloadService.copyAndCompress(in, out, true, "json");

    ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    ZipEntry entry = zipIn.getNextEntry();
    assertNotNull(entry);
    assertEquals("samples.json", entry.getName());
  }
}
