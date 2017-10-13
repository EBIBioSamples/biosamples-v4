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
	
	@Value("${biosamples.client.timeout:60000}")
	private int biosamplesClientTimeout;

	@Value("${biosamples.client.connectioncount.max:32}")
	private int connectionCountMax;
	
	@Value("${biosamples.client.connectioncount.default:32}")
	private int connectionCountDefault;

	@Value("${biosamples.client.threadcount:1}")
	private int threadCount;

	@Value("${biosamples.client.threadcount.max:32}")
	private int threadCountMax;
	
	@Value("${biosamples.client.aap.uri:https://explore.api.aap.tsi.ebi.ac.uk/auth}")
	private URI biosamplesClientAapUri;
	
	//can't use "null" because it will be a string
	@Value("${biosamples.client.aap.username:#{null}}")
	private String biosamplesClientAapUsername;

	//can't use "null" because it will be a string
	@Value("${biosamples.client.aap.password:#{null}}")
	private String biosamplesClientAapPassword;
	
	@Value("${biosamples.aap.super.read:self.BiosampleSuperUserRead}")
	private String biosamplesAapSuperRead;
	
	@Value("${biosamples.aap.super.write:self.BiosampleSuperUserWrite}")
	private String biosamplesAapSuperWrite;

	
	public URI getBiosamplesClientUri() {
		return biosamplesClientUri;
	}
	
	public int getBiosamplesClientPagesize() {
		return biosamplesClientPagesize;
	}
	
	public int getBiosamplesClientTimeout() {
		return biosamplesClientTimeout;
	}

	public int getBiosamplesClientConnectionCountMax() {
		return connectionCountMax;
	}

	public int getBiosamplesClientThreadCount() {
		return threadCount;
	}

	public int getBiosamplesClientThreadCountMax() {
		return threadCountMax;
	}

	public int getBiosamplesClientConnectionCountDefault() {
		return connectionCountDefault;
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
	
	public String getBiosamplesAapSuperRead() {
		return biosamplesAapSuperRead;
	}
	
	public String getBiosamplesAapSuperWrite() {
		return biosamplesAapSuperWrite;
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
