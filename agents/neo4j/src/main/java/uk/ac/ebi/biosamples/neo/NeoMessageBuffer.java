package uk.ac.ebi.biosamples.neo;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.messages.threaded.MessageBuffer;

@Component
public class NeoMessageBuffer extends MessageBuffer<MessageContent> {
	
	private final NeoMessageBufferTransaction neoMessageBufferTransaction;
	
	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	public NeoMessageBuffer(AgentNeo4JProperties properties, NeoMessageBufferTransaction neoMessageBufferTransaction) {
		super(properties.getAgentNeo4JQueueSize(), properties.getAgentNeo4JQueueTime());
		this.neoMessageBufferTransaction = neoMessageBufferTransaction;
	}

	@Override
	public void save(Collection<MessageContent> messageContents) {		
		neoMessageBufferTransaction.save(messageContents);
	}

}
