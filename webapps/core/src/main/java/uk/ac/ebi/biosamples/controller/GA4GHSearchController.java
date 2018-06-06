package com.example.simple_biosamples_client.controllers;

import com.example.simple_biosamples_client.utils.BiosamplesRetriever;
import com.example.simple_biosamples_client.models.SearchingForm;
import com.example.simple_biosamples_client.models.ga4ghmetadata.Biosample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;


@Controller
@EnableAutoConfiguration
public class GA4GHSearchController {

    @Autowired
    protected SearchingForm form;
    @Autowired
    protected BiosamplesRetriever accessPoint;


    @GetMapping("/SearchingForm")
    public String getSearchForm(Model model) {
        model.addAttribute("SearchingForm", form);
        return "SearchingForm";
    }

    @RequestMapping(value = "/SearchingForm/result", method = RequestMethod.GET)
    @ResponseBody
    public List<Biosample> submitForm(@Valid @ModelAttribute("SearchingForm") SearchingForm form) {
        return accessPoint.getFilteredSamplesBySearchForm(form);
    }
}
