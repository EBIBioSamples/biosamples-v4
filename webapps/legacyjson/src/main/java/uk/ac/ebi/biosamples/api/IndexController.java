package uk.ac.ebi.biosamples.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.ac.ebi.biosamples.client.BioSamplesClient;

import java.util.List;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@Controller
public class IndexController {

    Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    BioSamplesClient bioSamplesClient;

    @GetMapping(value = "/", produces = APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody
    ResourceSupport root() {
        IndexResource index = new IndexResource();
        TemplateVariables urlVars = new TemplateVariables(
                new TemplateVariable("page", TemplateVariable.VariableType.REQUEST_PARAM),
                new TemplateVariable("size", TemplateVariable.VariableType.REQUEST_PARAM_CONTINUED),
                new TemplateVariable("sort", TemplateVariable.VariableType.REQUEST_PARAM_CONTINUED)
        );

        // TODO finish to insert all links
        List<Link> resourceLinks = asList(
                new Link(new UriTemplate("externallinksrelations", urlVars),"externalrelations"),
                new Link(new UriTemplate("samples", urlVars), "samples"),
                new Link(new UriTemplate("groupsrelations", urlVars), "grouprelations")
        );
        index.add(resourceLinks);
        return index;

    }

}
