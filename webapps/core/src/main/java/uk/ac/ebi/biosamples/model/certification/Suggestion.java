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

import java.util.Arrays;
import java.util.Objects;

public class Suggestion {
  private String[] characteristic;
  private boolean mandatory;
  private String comment;

  public boolean isMandatory() {
    return mandatory;
  }

  public void setMandatory(boolean mandatory) {
    this.mandatory = mandatory;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String[] getCharacteristic() {
    return characteristic;
  }

  @Override
  public String toString() {
    return "Suggestion{"
        + "characteristic="
        + Arrays.toString(characteristic)
        + ", mandatory="
        + mandatory
        + ", comment='"
        + comment
        + '\''
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Suggestion)) return false;
    Suggestion that = (Suggestion) o;
    return isMandatory() == that.isMandatory()
        && Arrays.equals(getCharacteristic(), that.getCharacteristic())
        && Objects.equals(getComment(), that.getComment());
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(isMandatory(), getComment());
    result = 31 * result + Arrays.hashCode(getCharacteristic());
    return result;
  }

  public void setCharacteristic(String[] characteristic) {
    this.characteristic = characteristic;
  }
}
