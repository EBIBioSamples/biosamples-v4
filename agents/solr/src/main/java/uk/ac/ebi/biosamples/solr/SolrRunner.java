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
package uk.ac.ebi.biosamples.solr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.MessageUtils;
import uk.ac.ebi.biosamples.Messaging;

@Component
public class SolrRunner implements ApplicationRunner {
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Autowired private MessageUtils messageUtils;

  @Autowired private BioSamplesProperties biosamplesProperties;

  @Override
  public void run(final ApplicationArguments args) throws Exception {
    // as long as there are messages to read, keep this thread alive
    // that will also keep the async message client alive too?
    Long messageCount = null;
    while (biosamplesProperties.getAgentSolrStayalive()
        || messageCount == null
        || messageCount > 0) {
      Thread.sleep(1000);
      messageCount = messageUtils.getQueueCount(Messaging.INDEXING_QUEUE);
      log.trace("Messages remaining in " + Messaging.INDEXING_QUEUE + " " + messageCount);
    }
  }
}
