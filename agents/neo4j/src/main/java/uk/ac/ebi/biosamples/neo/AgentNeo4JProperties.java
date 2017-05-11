package uk.ac.ebi.biosamples.neo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AgentNeo4JProperties {

	@Value("${biosamples.agent.neo4j.stayalive:false}")
	private Boolean agentNeo4JStayalive;

	@Value("${biosamples.agent.neo4j.queuesize:1000}")
	private int agentNeo4JQueueSize;

	@Value("${biosamples.agent.neo4j.queuetime:1000}")
	private int agentNeo4JQueueTime;
	
	public int getAgentNeo4JQueueSize() {
		return agentNeo4JQueueSize;
	}
	
	public int getAgentNeo4JQueueTime() {
		return agentNeo4JQueueTime;
	}
	
	public boolean getAgentNeo4JStayalive() {
		return agentNeo4JStayalive;
	}

}
