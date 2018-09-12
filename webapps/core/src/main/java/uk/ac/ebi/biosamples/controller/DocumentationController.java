package uk.ac.ebi.biosamples.controller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.ebi.biosamples.BioSamplesProperties;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        this.cookbookResources = new Resource[]{};
        try {
            this.cookbookResources = resolver.getResources("classpath*:templates/asciidoc/cookbook_recipes/*");
            cookbookRecipiesMap = new HashMap<>();
            for(Resource res: cookbookResources) {
                Document doc = Jsoup.parse(res.getFile(), "UTF-8");
                Element title =  doc.getElementsByTag("title").first();
                String linkText = title.text();
                String linkUrl = res.getFilename().replaceFirst("cb_", "");
                cookbookRecipiesMap.put(linkText, linkUrl);
            }

        } catch (IOException e) {
            log.error("Unable to load cookbook resources", e);
        }
        this.bioSamplesProperties = properties;

    }

    //TODO: Convert this to use ControllerAdvice
    @ModelAttribute
    public void addCoreLink(Model model) {
        model.addAttribute("sampletabUrl", bioSamplesProperties.getBiosamplesWebappSampletabUri());
        model.addAttribute("recipes", this.cookbookRecipiesMap);
    }

    @GetMapping
    public String helpIndex() {
        return "docs/index";
    }

    @GetMapping(value = "/{page}")
    public String helpBasePage(@PathVariable String page) {
        return "docs/"+page;
    }

    @GetMapping(value = "/guides/")
    public String helpGuideIndex() {
        return "docs/guides/index";
   }

    @GetMapping(value = "/guides/{page}")
    public String helpGuidePage(@PathVariable String page) {
        return "docs/guides/"+page;
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
        return "docs/references/api/"+page;
    }

    @GetMapping(value = {"/cookbook", "/cookbook/"})
    public String cookBookIndex(){
        return "docs/cookbook/index";
    }

    @GetMapping(value = "/cookbook/{page}")
    public String cookbookPage(Model model, @PathVariable String page) {
        model.addAttribute("page", page);
        return "docs/cookbook/cookbook_template";
    }

}
