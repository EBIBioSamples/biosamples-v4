package uk.ac.ebi.biosamples.model.auth;

import java.io.Serializable;
import java.util.Objects;

public class AuthRequest implements Serializable {
    private String userName;
    private String password;
    private String loginWay;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthRequest)) return false;
        AuthRequest that = (AuthRequest) o;
        return Objects.equals(getUserName(), that.getUserName()) &&
                Objects.equals(getPassword(), that.getPassword());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserName(), getPassword());
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AuthRequest(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public AuthRequest(String userName, String password, String loginWay) {
        this.userName = userName;
        this.password = password;
        this.loginWay = loginWay;
    }

    public AuthRequest() {
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getLoginWay() {
        return loginWay;
    }

    public void setLoginWay(String loginWay) {
        this.loginWay = loginWay;
    }
}
