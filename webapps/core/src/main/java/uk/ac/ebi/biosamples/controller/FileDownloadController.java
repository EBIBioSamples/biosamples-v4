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
package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.biosamples.service.FileDownloadService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
@RequestMapping("/download")
public class FileDownloadController {
    private Logger log = LoggerFactory.getLogger(getClass());

    private final FileDownloadService fileDownloadService;

    public FileDownloadController(FileDownloadService fileDownloadService) {
        this.fileDownloadService = fileDownloadService;
    }

    @GetMapping
    public void download(HttpServletResponse response) {
        FileSystemResource resource = new FileSystemResource("config_test.json");
        try (ZipOutputStream zippedOut = new ZipOutputStream(response.getOutputStream())) {
            InputStream in = new ClassPathResource("config_test.json").getInputStream();
//            ZipEntry zipEntry = new ZipEntry("test.file.json");
//            zippedOut.putNextEntry(zipEntry);
//            StreamUtils.copy(in, zippedOut);

            ZipEntry zipEntry = new ZipEntry(resource.getFilename());
            zipEntry.setSize(resource.contentLength());
            zipEntry.setTime(System.currentTimeMillis());
            zippedOut.putNextEntry(zipEntry);
            StreamUtils.copy(resource.getInputStream(), zippedOut);

            zippedOut.closeEntry();
            zippedOut.finish();

        } catch (IOException e) {
            log.warn("Failed to download the file");
        }

        fileDownloadService.downloadCompressed();
    }
}
