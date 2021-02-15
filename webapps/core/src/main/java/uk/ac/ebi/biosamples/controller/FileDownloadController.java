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
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.FileDownloadService;
import uk.ac.ebi.biosamples.service.FilterService;
import uk.ac.ebi.biosamples.utils.LinkUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
@RequestMapping("/download")
public class FileDownloadController {
    private Logger log = LoggerFactory.getLogger(getClass());

    private final FileDownloadService fileDownloadService;
    private final FilterService filterService;
    private final BioSamplesAapService bioSamplesAapService;

    public FileDownloadController(FileDownloadService fileDownloadService,
                                  FilterService filterService,
                                  BioSamplesAapService bioSamplesAapService) {
        this.fileDownloadService = fileDownloadService;
        this.filterService = filterService;
        this.bioSamplesAapService = bioSamplesAapService;
    }

    @GetMapping
    public void download(@RequestParam(name = "text", required = false) String text,
                         @RequestParam(name = "filter", required = false) String[] filter,
                         @RequestParam(name = "zip", required = false, defaultValue = "true") boolean zip,
                         HttpServletResponse response, HttpServletRequest request) throws IOException {

        String decodedText = LinkUtils.decodeText(text);
        Collection<Filter> filters = filterService.getFiltersCollection(LinkUtils.decodeTexts(filter));
        Collection<String> domains = bioSamplesAapService.getDomains();

        String compress = request.getHeader("Accept-Encoding") != null ? request.getHeader("Accept-Encoding") : "";
        if (compress.contains("gzip")) {
            compress = setEncodingGzip(response);
        } else if (compress.contains("deflate")) {
            compress = setEncodingDeflate(response);
        } else if (compress.contains("zip")) {
            compress = setEncodingZip(response);
        }

        String accept = request.getHeader("Accept") != null ? request.getHeader("Accept") : "application/json";
        if (accept.contains("application/json")) {
            accept = "application/json";
        } else if (accept.contains("application/xml")) {
            accept = "application/xml";
        }

        InputStream in = fileDownloadService.getDownloadStream(decodedText, filters, domains, accept);
        OutputStream out = response.getOutputStream();

        if (zip) {
            fileDownloadService.copyCompressedStream(in, out, compress);
        } else {
            fileDownloadService.copyCompressedStream(in, out, "");
        }
        response.flushBuffer();
    }

    @GetMapping("/3")
    public void streamFile(HttpServletResponse response) {
        try {
            StreamUtils.copy(fileDownloadService.getDownloadStream("", Collections.emptyList(), Collections.emptyList(), "application/json"), response.getOutputStream());
            response.flushBuffer();

        } catch (IOException e) {
            e.printStackTrace();
            log.warn("Failed to download the file");
        }
    }

    @GetMapping("/4")
    public void streamCompressedFile(HttpServletResponse response) {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"samples.zip\"");
        try {
            InputStream in = fileDownloadService.getDownloadStream("", Collections.emptyList(), Collections.emptyList(), "application/json");
            OutputStream out = response.getOutputStream();
            fileDownloadService.copyCompressedStream(in, out, "zip");
            response.flushBuffer();
        } catch (IOException e) {
            e.printStackTrace();
            log.warn("Failed to download the file");
        }
    }

    private String setEncodingGzip(HttpServletResponse response) {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"samples.zip\"");
        return "gzip";
    }

    private String setEncodingDeflate(HttpServletResponse response) {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"samples.zip\"");
        return "deflate";
    }

    private String setEncodingZip(HttpServletResponse response) {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"samples.zip\"");
        return "zip";
    }
}
