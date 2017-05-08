package uk.ac.ebi.biosamples.messages.threaded;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MessageProperties {

	@Value("${biosamples.agent.queuesize:1000}")
	private int agentQueueSize;

	
	public int getAgentQueueSize() {
		return agentQueueSize;
	}
}
