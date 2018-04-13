package uk.ac.ebi.biosamples;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BioSamplesProperties {

    private static final String BIOSAMPLES_CLIENT_URI_STRING = "http://localhost:8081";
    private static final String BIOSAMPLES_CLIENT_AAP_URI_STRING = "https://explore.api.aap.tsi.ebi.ac.uk/auth";

    @Value("${biosamples.agent.solr.stayalive}")
    private Boolean agentSolrStayalive = false;

    @Value("${biosamples.client.uri}")
    private URI biosamplesClientUri;

    @Value("${biosamples.client.pagesize}")
    private int biosamplesClientPagesize = 1000;

    @Value("${biosamples.client.timeout}")
    private int biosamplesClientTimeout = 60000;

    @Value("${biosamples.client.connectioncount.max}")
    private int connectionCountMax = 8;

    @Value("${biosamples.client.connectioncount.default}")
    private int connectionCountDefault = 8;

    @Value("${biosamples.client.threadcount}")
    private int threadCount = 1;

    @Value("${biosamples.client.threadcount.max}")
    private int threadCountMax = 8;

    @Value("${biosamples.client.aap.uri}")
    private URI biosamplesClientAapUri;

    //can't use "null" because it will be a string
    @Value("${biosamples.client.aap.username:#{null}}")
    private String biosamplesClientAapUsername;

    //can't use "null" because it will be a string
    @Value("${biosamples.client.aap.password:#{null}}")
    private String biosamplesClientAapPassword;

    @Value("${biosamples.aap.super.read:self.BiosampleSuperUserRead}")
    private String biosamplesAapSuperRead = "self.BiosampleSuperUserRead";

    @Value("${biosamples.aap.super.write:self.BiosampleSuperUserWrite}")
    private String biosamplesAapSuperWrite = "self.BiosampleSuperUserWrite";

    @Value("${biosamples.ols}")
    private String ols = "https://wwwdev.ebi.ac.uk/ols";

    @Value("${biosamples.webapp.core.page.threadcount}")
    private int webappCorePageThreadCount = 64;

    @Value("${biosamples.webapp.core.page.threadcount.max}")
    private int webappCorePageThreadCountMax = 128;

    public BioSamplesProperties() {
        try {
            biosamplesClientUri = new URI(BIOSAMPLES_CLIENT_URI_STRING);
            biosamplesClientAapUri = new URI(BIOSAMPLES_CLIENT_AAP_URI_STRING);

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

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

    public void setBiosamplesClientUri(URI biosamplesClientUri) {
        this.biosamplesClientUri = biosamplesClientUri;
    }

    public void setBiosamplesClientAapUri(URI biosamplesClientAapUri) {
        this.biosamplesClientAapUri = biosamplesClientAapUri;
    }

    public void setBiosamplesClientAapUsername(String biosamplesClientAapUsername) {
        this.biosamplesClientAapUsername = biosamplesClientAapUsername;
    }

    public void setBiosamplesClientAapPassword(String biosamplesClientAapPassword) {
        this.biosamplesClientAapPassword = biosamplesClientAapPassword;
    }

}
