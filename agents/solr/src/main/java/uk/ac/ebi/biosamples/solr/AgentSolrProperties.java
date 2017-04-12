package uk.ac.ebi.biosamples.solr;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AgentSolrProperties {

	@Value("${biosamples.agent.solr.stayalive:false}")
	private Boolean agentSolrStayalive;

	
	public boolean getAgentSolrStayalive() {
		return agentSolrStayalive;
	}

}
