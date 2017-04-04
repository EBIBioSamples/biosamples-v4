package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class Neo4JRunner implements ApplicationRunner {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private MessageUtils messageUtils;
	
	@Autowired
	private AgentNeo4JProperties agentNeo4JProperties;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		// as long as there are messages to read, keep this thread alive
		// that will also keep the async message client alive too?
		Integer messageCount = null;
		while (agentNeo4JProperties.getAgentNeo4JStayalive() || messageCount == null || messageCount > 0) {
			Thread.sleep(1000);
			messageCount = messageUtils.getQueueCount(Messaging.queueToBeIndexedNeo4J);
			log.info("Messages remaining in "+Messaging.queueToBeIndexedNeo4J+" "+messageCount);
		}
		
	}

}
