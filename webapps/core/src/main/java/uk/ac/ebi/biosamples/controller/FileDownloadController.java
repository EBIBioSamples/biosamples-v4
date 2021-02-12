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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
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

    @GetMapping(value = "/0", produces="application/zip" )
    public void download(HttpServletResponse response) {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"samples.zip\"");

        try (ZipOutputStream zippedOut = new ZipOutputStream(response.getOutputStream())) {
            InputStream in = new ClassPathResource("config.json").getInputStream();
            ZipEntry zipEntry = new ZipEntry("samples.json");
            zippedOut.putNextEntry(zipEntry);
            StreamUtils.copy(in, zippedOut);

            zippedOut.closeEntry();
            zippedOut.finish();
        } catch (IOException e) {
            e.printStackTrace();
            log.warn("Failed to download the file");
        }

        fileDownloadService.downloadCompressed();
    }

    @GetMapping("/1")
    public void downloadFile(HttpServletResponse response) {
        try {
            InputStream in = new ClassPathResource("config.json").getInputStream();
            StreamUtils.copy(in, response.getOutputStream());
            response.flushBuffer();

        } catch (IOException e) {
            e.printStackTrace();
            log.warn("Failed to download the file");
        }
    }

    @GetMapping("/2")
    public void downloadFileWriteString(HttpServletResponse response) {
        try {
            InputStream in = fileDownloadService.writeStringToStream("", Collections.emptyList(), Collections.emptyList());
            StreamUtils.copy(in, response.getOutputStream());
            response.flushBuffer();

        } catch (IOException e) {
            e.printStackTrace();
            log.warn("Failed to download the file");
        }
    }

    @GetMapping("/3")
    public void streamFile(HttpServletResponse response) {
        try {

            StreamUtils.copy(fileDownloadService.getDownloadStream("", Collections.emptyList(), Collections.emptyList()), response.getOutputStream());
            response.flushBuffer();

        } catch (IOException e) {
            e.printStackTrace();
            log.warn("Failed to download the file");
        }
    }
}
