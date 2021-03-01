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

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

//    https://medium.com/swlh/streaming-data-with-spring-boot-restful-web-service-87522511c071
//    https://stackoverflow.com/questions/51845228/proper-way-of-streaming-using-responseentity-and-making-sure-the-inputstream-get
    @GetMapping(value = "/test3")
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

    @GetMapping(value = "/test1")
    public ResponseEntity<StreamingResponseBody> downloadTest1(@RequestParam(name = "text", required = false) String text,
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

//        OutputStream out = response.getOutputStream();
//        fileDownloadService.copyAndCompress(in, out, zip, format);
//        response.flushBuffer();

        StreamingResponseBody responseBody = outputStream -> {
            final ZipOutputStream zipOut = new ZipOutputStream(outputStream);
            try {
                final ZipEntry zipEntry = new ZipEntry("samples.json");
                zipOut.putNextEntry(zipEntry);
                byte[] bytes = new byte[1024];
                int length;
                while ((length = in.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
                in.close();
                zipOut.close();
            } catch (final IOException e) {
//                logger.error("Exception while reading and streaming data {} ", e);
                e.printStackTrace();
            }
        };

        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=generic_file_name.zip")
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(responseBody);
    }

    @GetMapping(value = "/test2")
    public ResponseEntity<StreamingResponseBody> downloadTest2(@RequestParam(name = "text", required = false) String text,
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

//        OutputStream out = response.getOutputStream();
//        fileDownloadService.copyAndCompress(in, out, zip, format);
//        response.flushBuffer();

        StreamingResponseBody responseBody = outputStream -> {
            final ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream());
            try {
                final ZipEntry zipEntry = new ZipEntry("samples.json");
                zipOut.putNextEntry(zipEntry);
                byte[] bytes = new byte[1024];
                int length;
                while ((length = in.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
                in.close();
                zipOut.close();
            } catch (final IOException e) {
//                logger.error("Exception while reading and streaming data {} ", e);
                e.printStackTrace();
            }
        };

        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=generic_file_name.zip")
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(responseBody);
    }

    @GetMapping
    public ResponseEntity<StreamingResponseBody> downloadTest3(@RequestParam(name = "text", required = false) String text,
                                                           @RequestParam(name = "filter", required = false) String[] filter,
                                                           @RequestParam(name = "zip", required = false, defaultValue = "true") boolean zip,
                                                           @RequestParam(name = "format", required = false) String format, // there is no easy way to set accept header in html for downloading large files
                                                           HttpServletResponse response, HttpServletRequest request) throws IOException {

        String decodedText = LinkUtils.decodeText(text);
        Collection<Filter> filters = filterService.getFiltersCollection(LinkUtils.decodeTexts(filter));
        Collection<String> domains = bioSamplesAapService.getDomains();

        String outputFormat = getDownloadFormat(format, request.getHeader("Accept"));
        setResponseHeaders(response, zip, format);
        InputStream in = fileDownloadService.getDownloadStream(decodedText, filters, domains, format);
        StreamingResponseBody responseBody = outputStream -> fileDownloadService.copyAndCompress(in, outputStream, zip, outputFormat);

        return ResponseEntity.ok()
                .body(responseBody);
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
