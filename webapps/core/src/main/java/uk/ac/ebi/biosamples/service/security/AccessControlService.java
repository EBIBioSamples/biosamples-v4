package uk.ac.ebi.biosamples.service.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.exception.AccessControlException;
import uk.ac.ebi.biosamples.model.AuthToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Service
public class AccessControlService {
    private final ObjectMapper objectMapper;

    public AccessControlService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuthToken extractToken(String token) {
        token = token.startsWith("Bearer ") ? token.split("Bearer ")[1] : token;

        String[] chunks = token.split("\\.");
        Base64.Decoder decoder = Base64.getDecoder();
        String header = new String(decoder.decode(chunks[0]));
        String payload = new String(decoder.decode(chunks[1]));

        AuthToken authToken;
        try {
            String algorithm = objectMapper.readTree(header).get("alg").asText();
            String authority;
            String user;
            List<String> roles;

            JsonNode node = objectMapper.readTree(payload);
            if (node.get("iss") != null) { // Only AAP has this in the token
                authority = "AAP";
                user = node.get("sub").asText();
                roles = objectMapper.convertValue(node.get("domains"), new TypeReference<List<String>>() {
                });
            } else {
                authority = "WEBIN";
                user = node.get("principle").asText();
                roles = objectMapper.convertValue(node.get("role"), new TypeReference<List<String>>() {
                });
            }

            authToken = new AuthToken(algorithm, authority, user, roles);
        } catch (IOException e) {
            throw new AccessControlException("Could not decode token. ", e);
        }

        return authToken;
    }

    public boolean verifySignature() {
        // todo without verifying with the authority we cant make any claims about the validity of the token
        return true;
    }

    public List<String> getUserRoles(AuthToken token) {
        return token.getAuthority().equals("AAP") ? token.getRoles() : Collections.singletonList(token.getUser());
    }

    public static void main(String[] args) throws Exception {
        AccessControlService acs = new AccessControlService(new ObjectMapper());
        acs.extractToken("eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2V4cGxvcmUuYWFpLmViaS5hYy51ay9zcCIsImp0aSI6IjVvWVdQY2FBc2R4eHAtZVVOUThydVEiLCJpYXQiOjE2MjY4NjM3NjIsInN1YiI6InVzci04NjYyYmEwNS0xMzRhLTRiMTQtODViMC04ZTUyY2I5ZmVlOGQiLCJlbWFpbCI6ImlzdXJ1QGViaS5hYy51ayIsIm5pY2tuYW1lIjoiaXN1cnVsIiwibmFtZSI6IklzdXJ1IExpeWFuYWdlIiwiZG9tYWlucyI6WyJzZWxmLklzdXJ1MSIsInN1YnMudGVzdC10ZWFtLTIzIiwic3Vicy5kZXYtdGVhbS0xNDAiXSwiZXhwIjoxNjI2OTUwMTYyfQ.YKxnlyZiwGzLCankVeqSNZc9Wa3SKZNBSks2EXlUKeqvuwZ9nNprodNTr1l99-KStvXWpl7ue56np1gsBIMtukO_hyOyrA3KTy36RZHO-sX_-RUSnKc7TpF7V6IqRbrPZj9RZs8HFaY5A5zYekyydSyfW3qNDev0PM4_CWirdBmhBfJ1HPXeMKy9dTULMGzskb3nItbrLRwF6nf6No5ClPzh2-g0rtd2nfS-GHMSbzYCYpv4NCR-kLkAn9-CzFDB-cXLFTgp4bp01YZ6UFG-EpMs8UMFROQxCLJxW78sMXr1WreYrhY8U9sECMJ-qK4uHo_nffGYcLUUh4RD20xvcw");
        acs.extractToken("eyJhbGciOiJSUzI1NiJ9.eyJwcmluY2lwbGUiOiJXZWJpbi01OTI4NyIsInJvbGUiOltdLCJleHAiOjE2MjY4ODE5MjgsImlhdCI6MTYyNjg2MzkyOH0.OkgsxRLGkG0O5nbVnVsgwKRNMM3Fqh4bsNRqM0n0fTWLqqqBcJ4tNgaihj7OmZmCpIKTOecxEhh3anNfjQQ1O9vQhtCeiFz9g2Tj8pTdv-6FBZ5t5gidz5W4GDsJ_8hDnXPge7Gk5ug3_GddDAWHvwJhuK_OR5oIIAf6SBeWNr9HKLpOQYcywYsrmKAFjTgA-wrGWtcR3qvFVDiQCpW2UzB8kzFVKdegIdrI2PgQnP5e0f5BoQ5V-qo7WBwn81bW7NkWHBXVecMab_UsKUyTMqNbsFY5TGJNj715a1Z_N6npkynGCpB3VbR5X6L3JVEnlhkBoCTE9zKUbfa3KLglYA");
    }
}
