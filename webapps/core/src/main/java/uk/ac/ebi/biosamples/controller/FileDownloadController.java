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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.FileDownloadService;
import uk.ac.ebi.biosamples.service.FilterService;
import uk.ac.ebi.biosamples.utils.LinkUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

@Controller
@RequestMapping("/download")
public class FileDownloadController {
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
                         @RequestParam(name = "format", required = false) String format, // there is no easy way to set accept header in html for downloading large files
                         HttpServletResponse response, HttpServletRequest request) throws IOException {

        String decodedText = LinkUtils.decodeText(text);
        Collection<Filter> filters = filterService.getFiltersCollection(LinkUtils.decodeTexts(filter));
        Collection<String> domains = bioSamplesAapService.getDomains();

        format = getDownloadFormat(format, request.getHeader("Accept"));
        setResponseHeaders(response, zip, format);
        InputStream in = fileDownloadService.getDownloadStream(decodedText, filters, domains, format);
        OutputStream out = response.getOutputStream();
        fileDownloadService.copyAndCompress(in, out, zip, format);
        response.flushBuffer();
    }

    private String getDownloadFormat(String format, String acceptHeader) {
        if (format == null || format.isEmpty()) {
            format = acceptHeader != null && acceptHeader.contains("xml") ? "xml" : "json";
        }
        return format;
    }

    private void setResponseHeaders(HttpServletResponse response, boolean zip, String format) {
        if (zip) {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"samples.zip\"");
        } else {
            response.setContentType("application/" + format);
            response.setHeader("Content-Disposition", "attachment; filename=\"samples." + format + "\"");
        }
    }
}
