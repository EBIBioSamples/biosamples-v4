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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.biosamples.service.upload.FileUploadService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/upload")
public class FileUploadController {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    FileUploadService fileUploadService;

    @PostMapping
    public void upload(@RequestParam("file") MultipartFile file, ModelMap model) throws IOException {
        log.info("In upload Controller : " + model.get("token"));

        fileUploadService.upload(file);
    }

    @GetMapping
    public void upload(Model model) {
        List<Integer> listOfValues = Arrays.asList(1, 2, 3, 4, 5);

        model.addAttribute("ids", listOfValues);
    }
}
