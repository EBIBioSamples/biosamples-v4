package uk.ac.ebi.biosamples.neo;

import java.util.Collections;

import org.neo4j.driver.v1.exceptions.TransientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;

import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.Messaging;

@Service
class MessageContentChannelCallback implements ChannelCallback<MessageContent> {

	Logger log = LoggerFactory.getLogger(getClass());
	
	private final NeoMessageBufferTransaction neoMessageBufferTransaction;
	private final ObjectMapper objectMapper;
	private final RabbitTemplate rabbitTemplate;

	public MessageContentChannelCallback(ObjectMapper objectMapper, NeoMessageBufferTransaction neoMessageBufferTransaction, RabbitTemplate rabbitTemplate) {
		this.neoMessageBufferTransaction = neoMessageBufferTransaction;
		this.objectMapper = objectMapper;
		this.rabbitTemplate = rabbitTemplate;
	}

	@Override
	@Transactional
	public MessageContent doInRabbit(Channel channel) throws Exception {
		GetResponse getResponse = channel.basicGet(Messaging.queueToBeIndexedNeo4J, false);
		MessageContent messageContent = null;
		
		if (getResponse == null) {
			log.warn("Null response");
			return null;
		} else if (getResponse.getBody() == null || getResponse.getBody().length == 0) {
			log.warn("Empty message "+getResponse);
		}
			
		messageContent = this.objectMapper.readerFor(MessageContent.class).readValue(getResponse.getBody());
		
		this.log.info("Got message "+messageContent);

		try {
			this.neoMessageBufferTransaction.save(Collections.singletonList(messageContent));
			/*
		} catch (TransientException e) {
			log.warn("Encountered TransientException "+e);
			channel.basicNack(getResponse.getEnvelope().getDeliveryTag(), false, true);
			return messageContent;
			*/
		} catch (Exception e) {
			log.warn("Encountered Exception "+e);
			channel.basicNack(getResponse.getEnvelope().getDeliveryTag(), false, true);
			return messageContent;
		}
		
		//send on the next queue
		rabbitTemplate.convertAndSend(Messaging.exchangeForIndexingSolr, "", messageContent);
		
		channel.basicAck(getResponse.getEnvelope().getDeliveryTag(), false);
		
		return messageContent;
	}
}