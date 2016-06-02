package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class LiveRunner implements ApplicationRunner {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private MessageUtils messageUtils;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		
		// as long as there are messages to read, keep this thread alive
		// that will also keep the async message client alive too?
		Integer messageCount = null;
		while (messageCount == null || messageCount > 0) {
			Thread.sleep(1000*60);
			messageCount = messageUtils.getQueueCount(Messaging.queueToBeIndexedNeo4J);
		}
		
	}

}
