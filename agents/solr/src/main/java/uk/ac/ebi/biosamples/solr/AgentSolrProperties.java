package uk.ac.ebi.biosamples.solr;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AgentSolrProperties {

	@Value("${biosamples.agent.solr.stayalive:false}")
	private Boolean agentSolrStayalive;

	@Value("${biosamples.agent.solr.queuesize:1000}")
	private int agentSolrQueueSize;

	@Value("${biosamples.agent.solr.queuetime:1000}")
	private int agentSolrQueueTime;
	
	public int getAgentSolrQueueSize() {
		return agentSolrQueueSize;
	}
	
	public int getAgentSolrQueueTime() {
		return agentSolrQueueTime;
	}
	
	public boolean getAgentSolrStayalive() {
		return agentSolrStayalive;
	}

}
