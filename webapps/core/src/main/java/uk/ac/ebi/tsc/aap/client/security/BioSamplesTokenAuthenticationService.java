package uk.ac.ebi.tsc.aap.client.security;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class BioSamplesTokenAuthenticationService extends TokenAuthenticationService {
    public BioSamplesTokenAuthenticationService(BioSamplesTokenHandler tokenHandler) {
        super(tokenHandler);
    }
}
