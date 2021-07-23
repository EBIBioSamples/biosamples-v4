package uk.ac.ebi.biosamples.service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.junit.Assert;
import uk.ac.ebi.biosamples.model.AuthToken;
import uk.ac.ebi.biosamples.model.auth.LoginWays;

import java.util.List;

public class AccessControlServiceTest extends TestCase {
    private static final String AAP_TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2V4cGxvcmUuYWFpLmViaS5hYy51a" +
            "y9zcCIsImp0aSI6IjVvWVdQY2FBc2R4eHAtZVVOUThydVEiLCJpYXQiOjE2MjY4NjM3NjIsInN1YiI6InVzci04NjYyYmEwNS0xM" +
            "zRhLTRiMTQtODViMC04ZTUyY2I5ZmVlOGQiLCJlbWFpbCI6ImlzdXJ1QGViaS5hYy51ayIsIm5pY2tuYW1lIjoiaXN1cnVsIiwib" +
            "mFtZSI6IklzdXJ1IExpeWFuYWdlIiwiZG9tYWlucyI6WyJzZWxmLklzdXJ1MSIsInN1YnMudGVzdC10ZWFtLTIzIiwic3Vicy5kZ" +
            "XYtdGVhbS0xNDAiXSwiZXhwIjoxNjI2OTUwMTYyfQ.YKxnlyZiwGzLCankVeqSNZc9Wa3SKZNBSks2EXlUKeqvuwZ9nNprodNTr1" +
            "l99-KStvXWpl7ue56np1gsBIMtukO_hyOyrA3KTy36RZHO-sX_-RUSnKc7TpF7V6IqRbrPZj9RZs8HFaY5A5zYekyydSyfW3qNDe" +
            "v0PM4_CWirdBmhBfJ1HPXeMKy9dTULMGzskb3nItbrLRwF6nf6No5ClPzh2-g0rtd2nfS-GHMSbzYCYpv4NCR-kLkAn9-CzFDB-c" +
            "XLFTgp4bp01YZ6UFG-EpMs8UMFROQxCLJxW78sMXr1WreYrhY8U9sECMJ-qK4uHo_nffGYcLUUh4RD20xvcw";
    private static final String WEBIN_TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJwcmluY2lwbGUiOiJXZWJpbi01OTI4NyIsInJvbGUiO" +
            "ltdLCJleHAiOjE2MjY4ODE5MjgsImlhdCI6MTYyNjg2MzkyOH0.OkgsxRLGkG0O5nbVnVsgwKRNMM3Fqh4bsNRqM0n0fTWLqqqBc" +
            "J4tNgaihj7OmZmCpIKTOecxEhh3anNfjQQ1O9vQhtCeiFz9g2Tj8pTdv-6FBZ5t5gidz5W4GDsJ_8hDnXPge7Gk5ug3_GddDAWHv" +
            "wJhuK_OR5oIIAf6SBeWNr9HKLpOQYcywYsrmKAFjTgA-wrGWtcR3qvFVDiQCpW2UzB8kzFVKdegIdrI2PgQnP5e0f5BoQ5V-qo7W" +
            "Bwn81bW7NkWHBXVecMab_UsKUyTMqNbsFY5TGJNj715a1Z_N6npkynGCpB3VbR5X6L3JVEnlhkBoCTE9zKUbfa3KLglYA";
    AccessControlService accessControlService = new AccessControlService(new ObjectMapper());

    public void testExtractToken() {
        AuthToken aapToken = accessControlService.extractToken(AAP_TOKEN);
        AuthToken webinToken = accessControlService.extractToken(WEBIN_TOKEN);
        Assert.assertEquals(aapToken.getAuthority(), LoginWays.AAP);
        Assert.assertEquals(webinToken.getAuthority(), LoginWays.WEBIN);
    }

    public void testGetUserRoles() {
        AuthToken aapToken = accessControlService.extractToken(AAP_TOKEN);
        List<String> userRoles = accessControlService.getUserRoles(aapToken);
        System.out.println(userRoles);
        Assert.assertTrue(userRoles.contains("subs.test-team-23"));
    }
}