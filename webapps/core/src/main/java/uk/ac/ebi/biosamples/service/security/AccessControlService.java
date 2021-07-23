package uk.ac.ebi.biosamples.service.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.exception.AccessControlException;
import uk.ac.ebi.biosamples.model.AuthToken;
import uk.ac.ebi.biosamples.model.auth.LoginWays;

import java.io.IOException;
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

        if(!verifySignature(token)) {
            throw new AccessControlException("Failed to verify the integrity of the token");
        }

        AuthToken authToken;
        try {
            String algorithm = objectMapper.readTree(header).get("alg").asText();
            LoginWays authority;
            String user;
            List<String> roles;

            JsonNode node = objectMapper.readTree(payload);
            if (node.get("iss") != null && "https://explore.aai.ebi.ac.uk/sp".equals(node.get("iss").asText())) {
                authority = LoginWays.AAP;
                user = node.get("sub").asText();
                roles = objectMapper.convertValue(node.get("domains"), new TypeReference<List<String>>() {
                });
            } else {
                authority = LoginWays.WEBIN;
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

    public boolean verifySignature(String token) {
        // todo without verifying with the authority we cant make any claims about the validity of the token
        return true;
    }

    public List<String> getUserRoles(AuthToken token) {
        return token.getAuthority() == LoginWays.AAP ? token.getRoles() : Collections.singletonList(token.getUser());
    }
}
