package uk.ac.ebi.biosamples.solr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.MessageUtils;
import uk.ac.ebi.biosamples.Messaging;

@Component
public class SolrRunner implements ApplicationRunner, ExitCodeGenerator {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private MessageUtils messageUtils;
	
	@Autowired
	private AgentSolrProperties agentSolrProperties;	

	//wire in the message buffer so we can return a non-zero exit code if there are any problems
	@Autowired
	private SolrMessageBuffer messageBuffer;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		// as long as there are messages to read, keep this thread alive
		// that will also keep the async message client alive too?
		Integer messageCount = null;
		while (agentSolrProperties.getAgentSolrStayalive() || messageCount == null || messageCount > 0) {
			Thread.sleep(1000);
			messageCount = messageUtils.getQueueCount(Messaging.queueToBeIndexedSolr);
			log.trace("Messages remaining in "+Messaging.queueToBeIndexedSolr+" "+messageCount);
		}
	}

	@Override
	public int getExitCode() {
		//exit code depends on message buffer
		boolean hadProblem = messageBuffer.hadProblem.get();
		if (hadProblem) {
			return 1;
		} else {
			return 0;
		}
	}

}
