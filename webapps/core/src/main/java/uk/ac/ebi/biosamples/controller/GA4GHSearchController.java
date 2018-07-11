package uk.ac.ebi.biosamples.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.ga4ghmetadata.Biosample;
import uk.ac.ebi.biosamples.service.ga4ghService.BiosamplesRetriever;
import uk.ac.ebi.biosamples.ga4ghmetadata.SearchingForm;

import javax.validation.Valid;
import java.util.List;


@Controller
public class GA4GHSearchController {

    @Autowired
    protected SearchingForm form;
    @Autowired
    protected BiosamplesRetriever accessPoint;


    @GetMapping("/ga4gh_search")
    public String getSearchForm(Model model) {
        model.addAttribute("SearchingForm", form);
        return "SearchingForm";
    }

    @RequestMapping(value = "/SearchingForm/result", method = RequestMethod.GET, produces={ MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    public List<Biosample> submitForm(@Valid @ModelAttribute("SearchingForm") SearchingForm form,
                                      @RequestParam(name = "page", required = false) final Integer page) {
        int requestedPage;
        if(page==null){
            requestedPage=0;
        } else {
            requestedPage = page;
        }return accessPoint.getFilteredPagedSamplesBySearchForm(form,requestedPage);
    }
}
