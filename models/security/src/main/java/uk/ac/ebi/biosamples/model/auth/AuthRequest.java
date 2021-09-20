/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
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
    return Objects.equals(getUserName(), that.getUserName())
        && Objects.equals(getPassword(), that.getPassword());
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

  public AuthRequest() {}

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
