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

@Controller
@RequestMapping("/docs")
public class DocumentationController {
  private Resource[] cookbookResources;
  private HashMap<String, String> cookbookRecipiesMap;

  private final Logger log = LoggerFactory.getLogger(getClass());

  public DocumentationController() {
    final ClassLoader cl = getClass().getClassLoader();
    final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
    cookbookResources = new Resource[] {};
    try {
      cookbookResources =
          resolver.getResources("classpath*:/templates/asciidoc/cookbook_recipes/*");
      cookbookRecipiesMap = new HashMap<>();
      for (final Resource res : cookbookResources) {
        final Document doc = Jsoup.parse(res.getInputStream(), "UTF-8", "");
        final Element title = doc.getElementsByTag("title").first();
        final String linkText = title.text();
        final String linkUrl = res.getFilename().replaceFirst("cb_", "");
        cookbookRecipiesMap.put(linkText, linkUrl);
      }

    } catch (final IOException e) {
      log.error("Unable to load cookbook resources", e);
    }
  }

  // TODO: Convert this to use ControllerAdvice
  @ModelAttribute
  public void addCoreLink(final Model model) {
    model.addAttribute("recipes", cookbookRecipiesMap);
  }

  @GetMapping
  public String helpIndex() {
    return "docs/index";
  }

  @GetMapping(value = "/{page}")
  public String helpBasePage(@PathVariable final String page) {
    return "docs/" + page;
  }

  @GetMapping(value = "/guides/")
  public String helpGuideIndex() {
    return "docs/guides/index";
  }

  @GetMapping(value = "/guides/{page}")
  public String helpGuidePage(@PathVariable final String page) {
    return "docs/guides/" + page;
  }

  @GetMapping(value = "/references/")
  public String helpReferenceIndex() {
    return "docs/references/overview";
  }

  @GetMapping(value = "/references/{page}")
  public String helpReferencePage(@PathVariable final String page) {
    return "docs/references/" + page;
  }

  @GetMapping(value = "/references/api")
  public String helpApiReferenceHome() {
    return "docs/references/api/overview";
  }

  @GetMapping(value = "/references/api/{page}")
  public String helpApiReferencePage(@PathVariable final String page) {
    return "docs/references/api/" + page;
  }

  @GetMapping(value = {"/cookbook", "/cookbook/"})
  public String cookBookIndex() {
    return "docs/cookbook/index";
  }

  @GetMapping(value = "/cookbook/{page}")
  public String cookbookPage(final Model model, @PathVariable final String page) {
    model.addAttribute("page", page);
    return "docs/cookbook/cookbook_template";
  }
}
