package uk.ac.ebi.biosamples.neo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DebugTransactionListener {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	@TransactionalEventListener
	public void listenToTransaction(ApplicationEvent event) {
		log.info("FOO TRANSACTION "+event);
		
	}
}
