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
package uk.ac.ebi.biosamples.model.certification;

public class CurationResult {
  private final String characteristic;
  private final String before;
  private final String after;
  private final boolean applied;

  public CurationResult(String characteristic) {
    this.applied = false;
    this.characteristic = characteristic;
    this.before = null;
    this.after = null;
  }

  public CurationResult(String characteristic, String before, String after) {
    this.applied = true;
    this.characteristic = characteristic;
    this.before = before;
    this.after = after;
  }

  public boolean isApplied() {
    return applied;
  }

  public String getCharacteristic() {
    return characteristic;
  }

  public String getBefore() {
    return before;
  }

  public String getAfter() {
    return after;
  }

  @Override
  public String toString() {
    return "CurationResult{"
        + "characteristic='"
        + characteristic
        + '\''
        + ", before='"
        + before
        + '\''
        + ", after='"
        + after
        + '\''
        + ", applied="
        + applied
        + '}';
  }
}
