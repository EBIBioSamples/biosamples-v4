/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.mongo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class MongoSampleTabApiKey {

  @Id private String apiKey;
  private String userName;
  private String publicEmail;
  private String publicUrl;
  private String contactName;
  private String contactEmail;
  private String aapDomain;
  private String gdprConsentDate;
  private String gdprConsentVersion;

  private MongoSampleTabApiKey(
      String apiKey,
      String userName,
      String publicEmail,
      String publicUrl,
      String contactName,
      String contactEmail,
      String aapDomain,
      String gdprConsentDate,
      String gdprConsentVersion) {
    super();
    this.apiKey = apiKey;
    this.userName = userName;
    this.publicEmail = publicEmail;
    this.publicUrl = publicUrl;
    this.contactName = contactName;
    this.contactEmail = contactEmail;
    this.aapDomain = aapDomain;
    this.gdprConsentDate = gdprConsentDate;
    this.gdprConsentVersion = gdprConsentVersion;
  }

  public String getApiKey() {
    return apiKey;
  }

  public String getUserName() {
    return userName;
  }

  public String getPublicEmail() {
    return publicEmail;
  }

  public String getPublicUrl() {
    return publicUrl;
  }

  public String getContactName() {
    return contactName;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public String getAapDomain() {
    return aapDomain;
  }

  public String getGdprConsentDate() {
    return gdprConsentDate;
  }

  public String getGdprConsentVersion() {
    return gdprConsentVersion;
  }

  public static MongoSampleTabApiKey build(
      String apiKey,
      String userName,
      String publicEmail,
      String publicUrl,
      String contactName,
      String contactEmail,
      String aapDomain,
      String gdprConsentDate,
      String gdprConsentVersion) {
    return new MongoSampleTabApiKey(
        apiKey,
        userName,
        publicEmail,
        publicUrl,
        contactName,
        contactEmail,
        aapDomain,
        gdprConsentDate,
        gdprConsentVersion);
  }
}
