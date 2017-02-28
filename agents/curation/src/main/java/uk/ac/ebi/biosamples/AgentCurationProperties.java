package uk.ac.ebi.biosamples;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AgentCurationProperties {

	@Value("${biosamples.agent.curation.stayalive:false}")
	private Boolean agentCurationStayalive;

	
	public boolean getAgentCurationStayalive() {
		return agentCurationStayalive;
	}

}
