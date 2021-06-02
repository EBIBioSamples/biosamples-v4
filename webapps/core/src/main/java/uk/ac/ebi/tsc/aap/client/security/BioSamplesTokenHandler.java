package uk.ac.ebi.tsc.aap.client.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.User;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class BioSamplesTokenHandler extends TokenHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public User parseUserFromToken(String token) {
        try {
            Set<Domain> domainsSet = new HashSet<>();
            JwtClaims jwtClaims = jwtConsumer.processToClaims(token);
            String userReference = jwtClaims.getSubject();
            String nickname = jwtClaims.getStringClaimValue("nickname");
            String email = jwtClaims.getStringClaimValue("email");
            String fullName = jwtClaims.getStringClaimValue("name");
            List<String> domains = jwtClaims.getStringListClaimValue("domains");
            domains.forEach(name -> domainsSet.add(new Domain(name, null, null)));
            return new User(nickname, email, userReference, fullName, domainsSet);
        } catch (InvalidJwtException | MalformedClaimException e) {
            return tryParsingWebinJwt(token);
        } catch (Exception e) {
            return tryParsingWebinJwt(token);
        }
    }

    private User tryParsingWebinJwt(String token) {
        try {
            final Claims claims = decodeJWT(token);

            if (claims == null) {
                throw new RuntimeException("No claims for this token");
            } else {
                return new User(null, null, claims.get("principle", String.class), null, null);
            }
        } catch (Exception e) {
            log.info("Cannot parse token: " + e.getMessage());
        }

        return new User(null, null, null, null, null);
    }

    public Claims decodeJWT(String jwt) {
        final int i = jwt.lastIndexOf('.');
        final String withoutSignature = jwt.substring(0, i+1);

        return Jwts.parser().parseClaimsJwt(withoutSignature).getBody();
    }
}
