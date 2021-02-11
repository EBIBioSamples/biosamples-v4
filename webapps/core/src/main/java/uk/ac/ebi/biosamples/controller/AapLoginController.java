package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.model.auth.AuthRequest;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.certification.CertifyService;
import uk.ac.ebi.tsc.aap.client.exception.UserNameOrPasswordWrongException;

import java.util.List;

@Controller
@RequestMapping("/login")
public class AapLoginController {
    private final BioSamplesAapService bioSamplesAapService;
    private final CertifyService certifyService;
    private Logger log = LoggerFactory.getLogger(getClass());

    public AapLoginController(BioSamplesAapService bioSamplesAapService, CertifyService certifyService) {
        this.bioSamplesAapService = bioSamplesAapService;
        this.certifyService = certifyService;
    }

    @PostMapping(value = "/auth")
    public String auth(@ModelAttribute("authRequest") AuthRequest authRequest, ModelMap model) {
        try {
            final String token = bioSamplesAapService.authenticate(authRequest.getUserName(), authRequest.getPassword());

            if (token != null) {
                List<String> domains = bioSamplesAapService.getDomains(token);
                List<String> certificates = certifyService.getCertificateNames();
                model.addAttribute("token", token);
                model.addAttribute("domains", domains);
                model.addAttribute("certificates", certificates);
                model.remove("wrongCreds");

                return "upload";
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
