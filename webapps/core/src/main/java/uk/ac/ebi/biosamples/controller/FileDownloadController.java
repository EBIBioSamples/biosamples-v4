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
package uk.ac.ebi.biosamples.controller;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FileDownloadService;
import uk.ac.ebi.biosamples.service.FilterService;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;
import uk.ac.ebi.biosamples.utils.LinkUtils;

@Controller
@RequestMapping("/download")
public class FileDownloadController {
  private static final Logger LOG = LoggerFactory.getLogger(FileDownloadController.class);

  private final FileDownloadService fileDownloadService;
  private final FilterService filterService;
  private final BioSamplesAapService bioSamplesAapService;

  public FileDownloadController(
      FileDownloadService fileDownloadService,
      FilterService filterService,
      BioSamplesAapService bioSamplesAapService) {
    this.fileDownloadService = fileDownloadService;
    this.filterService = filterService;
    this.bioSamplesAapService = bioSamplesAapService;
  }

  @GetMapping
  public ResponseEntity<StreamingResponseBody> download(
      @RequestParam(name = "text", required = false) String text,
      @RequestParam(name = "filter", required = false) String[] filter,
      @RequestParam(name = "zip", required = false, defaultValue = "true") boolean zip,
      @RequestParam(name = "format", required = false)
          String format, // there is no easy way to set accept header in html for downloading large
      // files
      @RequestParam(name = "count", required = false, defaultValue = "100000") int count,
      HttpServletResponse response,
      HttpServletRequest request) {
    LOG.info(
        "Sample bulk download request: text = {}, filters = {}", text, Arrays.toString(filter));
    String decodedText = LinkUtils.decodeSearchText(text);
    Collection<Filter> filters = filterService.getFiltersCollection(LinkUtils.decodeTexts(filter));
    Collection<String> domains = bioSamplesAapService.getDomains();

    String outputFormat = getDownloadFormat(format, request.getHeader("Accept"));
    setResponseHeaders(response, zip, outputFormat);
    InputStream in =
        fileDownloadService.getDownloadStream(decodedText, filters, domains, outputFormat, count);
    StreamingResponseBody responseBody =
        outputStream -> fileDownloadService.copyAndCompress(in, outputStream, zip, outputFormat);

    return ResponseEntity.ok().body(responseBody);
  }

  private String getDownloadFormat(String format, String acceptHeader) {
    if (format == null || format.isEmpty()) {
      format = acceptHeader != null && acceptHeader.contains("xml") ? "xml" : "json";
    }
    format = "accessions".equalsIgnoreCase(format) ? "txt" : format;
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
