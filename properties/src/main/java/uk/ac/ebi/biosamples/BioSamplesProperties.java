package uk.ac.ebi.biosamples;

import java.net.URI;

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

	
	@Value("${biosamples.client.uri:http://localhost:8081}")
	private URI biosamplesClientUri;

	@Value("${biosamples.client.pagesize:1000}")
	private int biosamplesClientPagesize;
	
	@Value("${biosamples.client.aap.uri:https://explore.api.aap.tsi.ebi.ac.uk/auth}")
	private URI biosamplesClientAapUri;
	
	//can't use "null" because it will be a string
	@Value("${biosamples.client.aap.username:#{null}}")
	private String biosamplesClientAapUsername;

	//can't use "null" because it will be a string
	@Value("${biosamples.client.aap.password:#{null}}")
	private String biosamplesClientAapPassword;

	
	public URI getBiosamplesClientUri() {
		return biosamplesClientUri;
	}
	
	public int getBiosamplesClientPagesize() {
		return biosamplesClientPagesize;
	}
	
	public URI getBiosamplesClientAapUri() {
		return biosamplesClientAapUri;
	}
	
	public String getBiosamplesClientAapUsername() {
		return biosamplesClientAapUsername;
	}
	
	public String getBiosamplesClientAapPassword() {
		return biosamplesClientAapPassword;
	}
	
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
