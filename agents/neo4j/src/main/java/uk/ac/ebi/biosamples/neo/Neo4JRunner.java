package uk.ac.ebi.biosamples.neo;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.MessageUtils;
import uk.ac.ebi.biosamples.Messaging;

@Component
public class Neo4JRunner implements ApplicationRunner, ExitCodeGenerator {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private BioSamplesProperties biosamplesProperties;
	@Autowired
	private Neo4JMessageHandler neo4JMessageHandler;
	
	private int exitCode = 0;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Starting run()");
		
		MessageContent messageContent = null;
		long lastMessage = System.nanoTime();
		long interval = System.nanoTime()-lastMessage;
		//TODO application property this
		while (interval < 15*1000000 || biosamplesProperties.getAgentNeo4JStayalive()) {
			log.info("Handling new message");
			messageContent = neo4JMessageHandler.handle();
			log.info("Handled "+messageContent);
			
			if (messageContent != null) {
				lastMessage = System.nanoTime();
			}
			interval = System.nanoTime()-lastMessage;
		} 
		
		log.info("Finishing run()");
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

}
