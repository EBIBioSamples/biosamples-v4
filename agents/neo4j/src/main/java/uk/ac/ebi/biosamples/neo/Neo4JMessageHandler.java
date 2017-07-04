package uk.ac.ebi.biosamples.neo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP.Basic;

import uk.ac.ebi.biosamples.MessageContent;

@Service
public class Neo4JMessageHandler {
	
	private final MessageContentChannelCallback messageContentChannelCallback;

	private final RabbitTemplate rabbitTemplate;
	
	public Neo4JMessageHandler(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper, 
			NeoMessageBufferTransaction neoMessageBufferTransaction) {
		this.rabbitTemplate = rabbitTemplate;
		this.messageContentChannelCallback = new MessageContentChannelCallback(objectMapper, neoMessageBufferTransaction, rabbitTemplate);
	}
	
	//@Transactional
	public MessageContent handle() {
		
		return rabbitTemplate.execute(messageContentChannelCallback);
	}
}
