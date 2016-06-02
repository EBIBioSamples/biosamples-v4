package uk.ac.ebi.biosamples;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageUtils {


	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private AmqpAdmin admin;

	protected Integer getQueueCount(final String name) {
        Properties props = admin.getQueueProperties(name);
        Integer messageCount = Integer.valueOf(props.get("QUEUE_MESSAGE_COUNT").toString());
        log.trace("QUEUE_MESSAGE_COUNT="+messageCount);
        return messageCount;
	}
}
