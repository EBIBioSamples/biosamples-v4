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
package uk.ac.ebi.biosamples.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.messaging.MessagingConstants;
import uk.ac.ebi.biosamples.messaging.service.MessageUtils;

@Component
public class FileUploadMessageQueueRunner implements ApplicationRunner {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Autowired private MessageUtils messageUtils;

  @Override
  public void run(final ApplicationArguments args) {
    while (true) {
      log.trace(
          "Messages remaining in "
              + MessagingConstants.UPLOAD_QUEUE
              + " "
              + messageUtils.getQueueCount(MessagingConstants.UPLOAD_QUEUE));
    }
  }
}
