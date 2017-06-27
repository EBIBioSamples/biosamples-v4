package uk.ac.ebi.biosamples.neo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.MessageUtils;
import uk.ac.ebi.biosamples.Messaging;

@Component
public class Neo4JRunner implements ApplicationRunner, ExitCodeGenerator {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private BioSamplesProperties biosamplesProperties;
<<<<<<< HEAD
=======

	//wire in the message buffer so we can return a non-zero exit code if there are any problems
>>>>>>> merged properties into a single submodule
	@Autowired
	private MessageUtils messageUtils;
	
	
	private int exitCode = 0;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		// as long as there are messages to read, keep this thread alive
		// that will also keep the async message client alive too?
<<<<<<< HEAD
		Long messageCount = null;
		while (biosamplesProperties.getAgentNeo4JStayalive() 
				|| messageCount == null || messageCount > 0 ) {
=======
		Integer messageCount = null;
		while (biosamplesProperties.getAgentNeo4JStayalive() 
				|| messageCount == null || messageCount > 0
				||!messageBuffer.areAllStored()) {
>>>>>>> merged properties into a single submodule
			Thread.sleep(1000);
			messageCount = messageUtils.getQueueCount(Messaging.queueToBeIndexedNeo4J);
			log.trace("Messages remaining in "+Messaging.queueToBeIndexedNeo4J+" "+messageCount);
		}
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

}
