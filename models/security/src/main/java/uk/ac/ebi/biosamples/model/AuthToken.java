package uk.ac.ebi.biosamples.model;

import uk.ac.ebi.biosamples.model.auth.LoginWays;

import java.util.List;

public class AuthToken {
    private String algorithm;
    private LoginWays authority;
    private String user;
    private String email;
    private List<String> roles;

    public AuthToken(String algorithm, LoginWays authority, String user, List<String> roles) {
        this.algorithm = algorithm;
        this.authority = authority;
        this.user = user;
        this.roles = roles;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public LoginWays getAuthority() {
        return authority;
    }

    public String getUser() {
        return user;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getRoles() {
        return roles;
    }
}
