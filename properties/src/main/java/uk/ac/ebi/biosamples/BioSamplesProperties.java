package uk.ac.ebi.biosamples;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BioSamplesProperties {

	@Value("${biosamples.agent.solr.stayalive:false}")
	private Boolean agentSolrStayalive;
	
	@Value("${biosamples.client.uri:http://localhost:8081}")
	private URI biosamplesClientUri;

	@Value("${biosamples.client.pagesize:1000}")
	private int biosamplesClientPagesize;
	
	@Value("${biosamples.client.timeout:60000}")
	private int biosamplesClientTimeout;

	@Value("${biosamples.client.connectioncount.max:8}")
	private int connectionCountMax;
	
	@Value("${biosamples.client.connectioncount.default:8}")
	private int connectionCountDefault;

	@Value("${biosamples.client.threadcount:1}")
	private int threadCount;

	@Value("${biosamples.client.threadcount.max:8}")
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
	
	@Value("${biosamples.ols:https://wwwdev.ebi.ac.uk/ols}")
	private String ols;

	@Value("${biosamples.webapp.core.page.threadcount:64}")
	private int webappCorePageThreadCount;

	@Value("${biosamples.webapp.core.page.threadcount.max:128}")
	private int webappCorePageThreadCountMax;

	
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
		
	public boolean getAgentSolrStayalive() {
		return agentSolrStayalive;
	}
	
	public String getOls() {
		return ols;
	}

	public int getBiosamplesCorePageThreadCount() {
		return webappCorePageThreadCount;
	}

	public int getBiosamplesCorePageThreadCountMax() {
		return webappCorePageThreadCountMax;
	}
	
}
