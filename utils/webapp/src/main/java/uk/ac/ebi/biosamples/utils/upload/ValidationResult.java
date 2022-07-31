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
package uk.ac.ebi.biosamples.utils.upload;

import java.util.*;

public class ValidationResult {
  private List<ValidationMessage> validationMessagesList = new ArrayList<>();

  public void addValidationMessage(final ValidationMessage message) {
    validationMessagesList.add(message);
  }

  public List<ValidationMessage> getValidationMessagesList() {
    return validationMessagesList;
  }

  public void clear() {
    validationMessagesList.clear();
  }

  public static class ValidationMessage {
    private String messageKey;
    private String messageValue;

    public ValidationMessage(String messageKey, String messageValue) {
      this.messageKey = messageKey;
      this.messageValue = messageValue;
    }

    public String getMessageKey() {
      return messageKey;
    }

    public String getMessageValue() {
      return messageValue;
    }
  }
}
