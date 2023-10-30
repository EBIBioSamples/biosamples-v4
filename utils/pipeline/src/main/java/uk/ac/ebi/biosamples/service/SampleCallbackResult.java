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
package uk.ac.ebi.biosamples.service;

import java.sql.Date;

public class SampleCallbackResult {
  private String biosampleId;
  private int statusId;
  private String egaId;
  private Date lastUpdated;

  public String getBiosampleId() {
    return biosampleId;
  }

  public void setBiosampleId(final String biosampleId) {
    this.biosampleId = biosampleId;
  }

  public int getStatusId() {
    return statusId;
  }

  public void setStatusId(final int statusId) {
    this.statusId = statusId;
  }

  public String getEgaId() {
    return egaId;
  }

  public void setEgaId(final String egaId) {
    this.egaId = egaId;
  }

  public Date getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(final Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }
}
