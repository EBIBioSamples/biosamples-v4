package uk.ac.ebi.biosamples.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.model.auth.AuthRealm;
import uk.ac.ebi.biosamples.model.auth.AuthRequest;
import uk.ac.ebi.biosamples.model.auth.AuthRequestWebin;
import uk.ac.ebi.biosamples.model.auth.SubmissionAccount;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.BioSamplesWebinAuthenticationService;
import uk.ac.ebi.biosamples.service.certification.CertifyService;
import uk.ac.ebi.tsc.aap.client.exception.UserNameOrPasswordWrongException;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/login")
public class LoginController {
    private final BioSamplesAapService bioSamplesAapService;
    private final CertifyService certifyService;
    private final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    ObjectMapper objectMapper;

    public LoginController(BioSamplesAapService bioSamplesAapService, CertifyService certifyService, BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService) {
        this.bioSamplesAapService = bioSamplesAapService;
        this.certifyService = certifyService;
        this.bioSamplesWebinAuthenticationService = bioSamplesWebinAuthenticationService;
    }

    @SneakyThrows
    @PostMapping(value = "/auth")
    public String auth(@ModelAttribute("authRequest") AuthRequest authRequest, ModelMap model) {
        try {
            log.info("Login way is " + authRequest.getLoginWay());
            List<String> certificates = certifyService.getAllCertificateNames();

            if (authRequest.getLoginWay().equals("WEBIN")) {
                final AuthRequestWebin authRequestWebin = new AuthRequestWebin(authRequest.getUserName(), authRequest.getPassword(), Arrays.asList(AuthRealm.ENA));
                final String token = bioSamplesWebinAuthenticationService.getWebinToken(objectMapper.writeValueAsString(authRequestWebin)).getBody();
                final SubmissionAccount submissionAccount = bioSamplesWebinAuthenticationService.getWebinSubmissionAccount(token).getBody();

                model.addAttribute("loginmethod", "WEBIN");
                model.addAttribute("certificates", certificates);
                model.addAttribute("webinAccount", submissionAccount.getId());

                return "upload";
            } else {
                final String token = bioSamplesAapService.authenticate(authRequest.getUserName(), authRequest.getPassword());

                if (token != null) {
                    List<String> domains = bioSamplesAapService.getDomains(token);
                    model.addAttribute("loginmethod", null);
                    model.addAttribute("domains", domains);
                    model.addAttribute("webinAccount", null);
                    model.addAttribute("certificates", certificates);
                    model.remove("wrongCreds");

                    return "upload";
                }
            }

            return "uploadLogin";
        } catch (Exception e) {
            if (e instanceof UserNameOrPasswordWrongException) {
                model.addAttribute("wrongCreds", "wrongCreds");
                return "uploadLogin";
            } else {
                model.addAttribute("wrongCreds", "wrongCreds");
                return "uploadLogin";
            }
        }
    }

    @GetMapping(value = "/domains")
    public @ResponseBody
    List<String> getDomains(@RequestBody String token) {
        return bioSamplesAapService.getDomains(token);
    }
}
