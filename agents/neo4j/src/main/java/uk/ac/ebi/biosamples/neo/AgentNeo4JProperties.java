package uk.ac.ebi.biosamples.neo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AgentNeo4JProperties {

	@Value("${biosamples.agent.neo4j.stayalive:false}")
	private Boolean agentNeo4JStayalive;

	
	public boolean getAgentNeo4JStayalive() {
		return agentNeo4JStayalive;
	}

}
