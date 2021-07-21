package uk.ac.ebi.biosamples.model;

import java.util.List;

public class AuthToken {
    private String algorithm;
    private String authority;
    private String user;
    private String email;
    private List<String> roles;

    public AuthToken(String algorithm, String authority, String user, List<String> roles) {
        this.algorithm = algorithm;
        this.authority = authority;
        this.user = user;
        this.roles = roles;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getAuthority() {
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
