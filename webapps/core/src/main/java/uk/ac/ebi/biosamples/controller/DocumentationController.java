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

import java.io.IOException;
import java.util.HashMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.biosamples.BioSamplesProperties;

@Controller
@RequestMapping("/docs")
public class DocumentationController {

  private BioSamplesProperties bioSamplesProperties;
  private Resource[] cookbookResources;
  private HashMap<String, String> cookbookRecipiesMap;

  Logger log = LoggerFactory.getLogger(getClass());

  public DocumentationController(BioSamplesProperties properties) {
    ClassLoader cl = this.getClass().getClassLoader();
    ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
    this.cookbookResources = new Resource[] {};
    try {
      this.cookbookResources =
          resolver.getResources("classpath*:/templates/asciidoc/cookbook_recipes/*");
      cookbookRecipiesMap = new HashMap<>();
      for (Resource res : cookbookResources) {
        Document doc = Jsoup.parse(res.getInputStream(), "UTF-8", "");
        Element title = doc.getElementsByTag("title").first();
        String linkText = title.text();
        String linkUrl = res.getFilename().replaceFirst("cb_", "");
        cookbookRecipiesMap.put(linkText, linkUrl);
      }

    } catch (IOException e) {
      log.error("Unable to load cookbook resources", e);
    }
    this.bioSamplesProperties = properties;
  }

  // TODO: Convert this to use ControllerAdvice
  @ModelAttribute
  public void addCoreLink(Model model) {
    model.addAttribute("recipes", this.cookbookRecipiesMap);
  }

  @GetMapping
  public String helpIndex() {
    return "docs/index";
  }

  @GetMapping(value = "/{page}")
  public String helpBasePage(@PathVariable String page) {
    return "docs/" + page;
  }

  @GetMapping(value = "/guides/")
  public String helpGuideIndex() {
    return "docs/guides/index";
  }

  @GetMapping(value = "/guides/{page}")
  public String helpGuidePage(@PathVariable String page) {
    return "docs/guides/" + page;
  }

  @GetMapping(value = "/references/")
  public String helpReferenceIndex() {
    return "docs/references/overview";
  }

  @GetMapping(value = "/references/{page}")
  public String helpReferencePage(@PathVariable String page) {
    return "docs/references/" + page;
  }

  @GetMapping(value = "/references/api")
  public String helpApiReferenceHome() {
    return "docs/references/api/overview";
  }

  @GetMapping(value = "/references/api/{page}")
  public String helpApiReferencePage(@PathVariable String page) {
    return "docs/references/api/" + page;
  }

  @GetMapping(value = {"/cookbook", "/cookbook/"})
  public String cookBookIndex() {
    return "docs/cookbook/index";
  }

  @GetMapping(value = "/cookbook/{page}")
  public String cookbookPage(Model model, @PathVariable String page) {
    model.addAttribute("page", page);
    return "docs/cookbook/cookbook_template";
  }
}
