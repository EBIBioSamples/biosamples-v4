package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

@Component
public class MessageUtils {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	private Logger log = LoggerFactory.getLogger(this.getClass());
	

	public long getQueueCount(String queue) {
		long count = rabbitTemplate.execute(new ChannelCallback<Long>() {
			@Override
			public Long doInRabbit(Channel channel) throws Exception {
				return channel.messageCount(queue);
			}			
		});
		log.trace("Number of messages in "+queue+" = "+count);
		return count;
	}
}
