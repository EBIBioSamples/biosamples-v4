package uk.ac.ebi.biosamples;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BioSamplesProperties {

	//TODO merge these
	@Value("${biosamples.agent.curation.stayalive:false}")
	private Boolean agentCurationStayalive;
	@Value("${biosamples.agent.neo4j.stayalive:false}")
	private Boolean agentNeo4JStayalive;
	@Value("${biosamples.agent.solr.stayalive:false}")
	private Boolean agentSolrStayalive;

	@Value("${biosamples.agent.neo4j.queuesize:1000}")
	private int agentNeo4JQueueSize;

	@Value("${biosamples.agent.neo4j.queuetime:1000}")
	private int agentNeo4JQueueTime;

	@Value("${biosamples.agent.solr.queuesize:1000}")
	private int agentSolrQueueSize;

	@Value("${biosamples.agent.solr.queuetime:1000}")
	private int agentSolrQueueTime;
	
	public boolean getAgentCurationStayalive() {
		return agentCurationStayalive;
	}
	
	public int getAgentNeo4JQueueSize() {
		return agentNeo4JQueueSize;
	}
	
	public int getAgentNeo4JQueueTime() {
		return agentNeo4JQueueTime;
	}
	
	public boolean getAgentNeo4JStayalive() {
		return agentNeo4JStayalive;
	}
	
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
