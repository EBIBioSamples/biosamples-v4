package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.model.auth.AuthRequest;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.tsc.aap.client.exception.UserNameOrPasswordWrongException;

import java.util.List;

@Controller
@RequestMapping("/login")
public class AapLoginController {
    private final BioSamplesAapService bioSamplesAapService;
    private Logger log = LoggerFactory.getLogger(getClass());

    public AapLoginController(BioSamplesAapService bioSamplesAapService) {
        this.bioSamplesAapService = bioSamplesAapService;
    }

    @PostMapping(value = "/auth")
    public String auth(@ModelAttribute("authRequest") AuthRequest authRequest, ModelMap model) {
        try {
            final String token = bioSamplesAapService.authenticate(authRequest.getUserName(), authRequest.getPassword());

            if (token != null) {
                model.addAttribute("token", token);
                model.remove("wrongCreds");

                return "redirect:/upload";
            }

            return "uploadLogin";
        } catch (UserNameOrPasswordWrongException e) {
            model.addAttribute("wrongCreds", "wrongCreds");
            return "uploadLogin";
        }
    }

    @GetMapping(value = "/domains")
    public @ResponseBody
    List<String> getDomains(@RequestBody String token) {
        return bioSamplesAapService.getDomains(token);
    }
}