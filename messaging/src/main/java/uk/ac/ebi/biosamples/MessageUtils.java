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
package uk.ac.ebi.biosamples;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageUtils {

  @Autowired private RabbitTemplate rabbitTemplate;

  private Logger log = LoggerFactory.getLogger(this.getClass());

  public long getQueueCount(String queue) {
    long count =
        rabbitTemplate.execute(
            new ChannelCallback<Long>() {
              @Override
              public Long doInRabbit(Channel channel) throws Exception {
                return channel.messageCount(queue);
              }
            });
    log.trace("Number of messages in " + queue + " = " + count);
    return count;
  }
}
