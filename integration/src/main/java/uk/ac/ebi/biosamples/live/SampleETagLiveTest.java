package uk.ac.ebi.biosamples.live;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class SampleETagLiveTest {

    private static final String BASE_URL = "http://localhost:8081/biosamples/samples/";

    private String token = null;

    private String BIOSAMPLES_CLIENT_AAP_URL = System.getenv("BIOSAMPLES_CLIENT_AAP_URL");
    private String BIOSAMPLES_CLIENT_AAP_USERNAME = System.getenv("BIOSAMPLES_CLIENT_AAP_USERNAME");
    private String BIOSAMPLES_CLIENT_AAP_PASSWORD = System.getenv("BIOSAMPLES_CLIENT_AAP_PASSWORD");

    private String getAapToken() throws ClientProtocolException, IOException {
        if (token == null) {
            CloseableHttpClient client = HttpClients.createDefault();
            String auth = BIOSAMPLES_CLIENT_AAP_USERNAME + ":" + BIOSAMPLES_CLIENT_AAP_PASSWORD;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(Charset.forName("US-ASCII")));
            String authHeader = "Basic " + new String(encodedAuth);
            HttpGet httpGet = new HttpGet(BIOSAMPLES_CLIENT_AAP_URL);
            httpGet.setHeader("Authorization", authHeader);
            CloseableHttpResponse response = client.execute(httpGet);
            String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
            client.close();
            token = responseString;
        }
        return token;
    }

    private String createSample() throws ClientProtocolException, IOException {
        String jwt = getAapToken();
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(BASE_URL);
        String json = "{\n" +
                "  \"name\" : \"FakeSample\",\n" +
                "  \"update\" : \"2018-07-25T09:24:14.951Z\",\n" +
                "  \"release\" : \"2018-07-25T09:24:14.951Z\",\n" +
                "  \"domain\" : \"self.ExampleDomain\"\n" +
                "}";
        StringEntity entity = new StringEntity(json);
        httpPost.setEntity(entity);
        httpPost.setHeader("Content-type", "application/json");
        httpPost.setHeader("Authorization", "Bearer " + jwt);
        CloseableHttpResponse response = client.execute(httpPost);
        assertThat(response.getStatusLine().getStatusCode(), equalTo(201));
        String url = response.getFirstHeader("Location").getValue();
        client.close();
        return url;
    }

    private void updateSample(String sampleUrl) throws ClientProtocolException, IOException {
        String jwt = getAapToken();
        CloseableHttpClient client = HttpClients.createDefault();
        String[] parts = sampleUrl.split("/");
        String accession = parts[parts.length - 1];
        HttpPut httpPut = new HttpPut(sampleUrl);
        String json = "{\n" +
                "  \"name\" : \"FakeSample\",\n" +
                "  \"accession\" : \"" + accession + "\",\n" +
                "  \"domain\" : \"self.ExampleDomain\",\n" +
                "  \"release\" : \"2018-07-25T09:24:14.763Z\",\n" +
                "  \"update\" : \"2018-07-25T09:24:14.763Z\",\n" +
                "  \"characteristics\" : { },\n" +
                "  \"releaseDate\" : \"2018-07-25\",\n" +
                "  \"updateDate\" : \"2018-07-25\"\n" +
                "}";
        StringEntity entity = new StringEntity(json);
        httpPut.setEntity(entity);
        httpPut.setHeader("Content-type", "application/json");
        httpPut.setHeader("Authorization", "Bearer " + jwt);
        CloseableHttpResponse response = client.execute(httpPut);
        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
    }

    @Test
    public void ensure_etag_header_is_returned_with_sample() throws ClientProtocolException, IOException {
        String sampleUrl = createSample();
        final HttpUriRequest request = new HttpGet(sampleUrl);
        final HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
        assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_OK));
        assertNotNull(httpResponse.getFirstHeader("ETag"));
    }

    /*
    If someone updates the sample, the ETAG is different
     */
    @Test
    public void ensure_etag_header_changes_with_sample_changes() throws ClientProtocolException, IOException {
        String sampleUrl = createSample();
        final HttpResponse httpResponse = HttpClientBuilder.create().build().execute(new HttpGet(sampleUrl));
        String originalEtag = httpResponse.getFirstHeader("ETag").getValue();
        updateSample(sampleUrl);
        final HttpResponse updatedHttpResponse = HttpClientBuilder.create().build().execute(new HttpGet(sampleUrl));
        String updatedEtag = updatedHttpResponse.getFirstHeader("ETag").getValue();
        assertThat(originalEtag, not(equalTo(updatedEtag)));
    }


    /*
    Verify that if I'm querying the sample multiple time I'm getting the same ETag
     */
    @Test
    public void ensure_consistent_etag_header_value_is_returned_with_sample() throws
            ClientProtocolException, IOException {
        String sampleUrl = createSample();
        final HttpUriRequest request = new HttpGet(sampleUrl);
        String previousEtagValue = null;
        for (int i = 0; i < 5; i++) {
            final HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
            String etagValue = httpResponse.getFirstHeader("ETag").getValue();
            if (previousEtagValue != null) {
                assertThat(etagValue, equalTo(previousEtagValue));
            }
            previousEtagValue = etagValue;
        }
    }

    /*
    If someone curate the sample with a specific curation object, the ETAG for the curated sample is different, but the ETA for the raw sample remains the same
     */
    @Test
    public void ensure_different_etag_header_value_is_returned_with_sample_when_in_curation_domain() throws
            ClientProtocolException, IOException {
        String sampleUrl = createSample();
        String withoutCurationDomainEtagValue = HttpClientBuilder.create().build().execute(new HttpGet(sampleUrl)).getFirstHeader("ETag").getValue();
        String withCurationDomainEtagValue = HttpClientBuilder.create().build().execute(new HttpGet(sampleUrl + "?curationdomain")).getFirstHeader("ETag").getValue();
        String withoutCurationDomainAgainEtagValue = HttpClientBuilder.create().build().execute(new HttpGet(sampleUrl)).getFirstHeader("ETag").getValue();
        assertThat(withoutCurationDomainEtagValue, not(equalTo(withCurationDomainEtagValue)));
        assertThat(withoutCurationDomainEtagValue, equalTo(withoutCurationDomainAgainEtagValue));
    }

}
