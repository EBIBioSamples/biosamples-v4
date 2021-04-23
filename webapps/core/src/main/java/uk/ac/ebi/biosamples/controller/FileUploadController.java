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

import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.biosamples.service.upload.FileUploadService;
import uk.ac.ebi.biosamples.service.upload.IsaTabUploadService;

@Controller
@RequestMapping("/upload")
public class FileUploadController {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    FileUploadService fileUploadService;

    @Autowired
    IsaTabUploadService isaTabUploadService;

    @PostMapping
    public ResponseEntity<byte[]> upload(
            @RequestParam("file") MultipartFile file,
            @Valid String hiddenAapDomain,
            @Valid String hiddenCertificate,
            @Valid String webinAccount,
            HttpServletResponse response,
            HttpServletRequest request)
            throws IOException {
        final File downloadableFile = isaTabUploadService.upload(file, hiddenAapDomain, hiddenCertificate, webinAccount);
        final byte[] bytes = FileUtils.readFileToByteArray(downloadableFile);
        final HttpHeaders headers = setResponseHeaders(downloadableFile);

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    private HttpHeaders setResponseHeaders(File file) {
        final HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.setContentType(new MediaType("text", "csv"));
        httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());

        return httpHeaders;
    }
}
