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
package uk.ac.ebi.biosamples.model.certification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Checklist {
  private String name;
  private String version;

  @JsonProperty(value = "file")
  private String fileName;

  private boolean block;

  public Checklist(String name, String version, String fileName, boolean block) {
    this.name = name;
    this.version = version;
    this.fileName = fileName;
    this.block = block;
  }

  private Checklist() {}

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  @JsonIgnore
  public String getFileName() {
    return fileName;
  }

  @JsonIgnore
  public String getID() {
    return String.format("%s-%s", name, version);
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public boolean isBlock() {
    return block;
  }

  public void setBlock(boolean block) {
    this.block = block;
  }

  @Override
  public String toString() {
    return getID();
  }
}
