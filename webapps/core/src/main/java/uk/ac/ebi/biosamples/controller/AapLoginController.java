package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.model.auth.AuthRequest;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;

@RestController
@RequestMapping("/aap-login")
public class AapLoginController {
    private final BioSamplesAapService bioSamplesAapService;
    private Logger log = LoggerFactory.getLogger(getClass());

    public AapLoginController(BioSamplesAapService bioSamplesAapService) {
        this.bioSamplesAapService = bioSamplesAapService;
    }

    @GetMapping(value = "/auth")
    public @ResponseBody String auth(@RequestBody AuthRequest authRequest) {
        return bioSamplesAapService.authenticate(authRequest.getUserName(), authRequest.getPassword());
    }
}
