package uk.ac.ebi.biosamples;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageUtils {


	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private AmqpAdmin admin;

	public Integer getQueueCount(final String name) {
		
        Properties props = admin.getQueueProperties(name);
        for (Object key : props.keySet()) {
        	if (key instanceof String) {
        		log.info("AMQP property "+key+" = "+props.getProperty((String)key));
        	}
        }
        Integer messageCount = Integer.valueOf(props.get("QUEUE_MESSAGE_COUNT").toString());
        log.trace("QUEUE_MESSAGE_COUNT="+messageCount);
        return messageCount;
	}
}
