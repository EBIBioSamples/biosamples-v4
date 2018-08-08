package uk.ac.ebi.biosamples;

import org.junit.*;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.biosamples.model.ENAHtsgetTicket;
import uk.ac.ebi.biosamples.service.ENAHtsgetService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.JsonBody.json;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;



public class ENAHtsgetServiceTest {

    @Autowired
    ENAHtsgetService htsgetService;
    private static ClientAndServer mockServer;

    @BeforeClass
    public static void startServer() {
        mockServer = startClientAndServer(8086);
    }

    @AfterClass
    public static void stopServer() {
        mockServer.stop();
    }
    public ENAHtsgetServiceTest() {
        htsgetService = new ENAHtsgetService();
    }

    private void createExpectationForBamFile() {
        new MockServerClient("localhost", 8086)
                .when(
                        request()
                        .withMethod("GET")
                        .withPath("/ga4gh/sample/SAMN07666497")
                        .withQueryStringParameter("format","BAM"),Times.exactly(1)
                )
                .respond(HttpResponse.response().withStatusCode(200)
                        .withHeaders(
                                new Header("Content-Type", "application/json; charset=utf-8"))
                        .withBody(json("{\"htsget\":{\"format\":\"BAM\",\"urls\":[{\"url\":\"localhost:8086/sample?accession=SAMN07666497&format=BAM&part=1\"}],\"md5Hash\":\"bd1a36383f9b5b7ff5a8df79e85de245\"}}")));
    }

    private void createExpectationForCramFile() {
        new MockServerClient("localhost", 8086)
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/ga4gh/sample/SAMN07666497")
                                .withQueryStringParameter("format","CRAM"),Times.exactly(1)
                )
                .respond(HttpResponse.response().withStatusCode(200)
                        .withHeaders(
                                new Header("Content-Type", "application/json; charset=utf-8"))
                        .withBody(json("{\"htsget\":{\"format\":\"CRAM\",\"urls\":[{\"url\":\"localhost:8086/sample?accession=SAMN07666497&format=CRAM&part=1\"}],\"md5Hash\":\"bd1a36383f9b5b7ff5a8df79e85de245\"}}")));
    }
    @Test
    public void bamTicketTest() {
        createExpectationForBamFile();
        ENAHtsgetTicket ticket = htsgetService.getTicket("SAMN07666497", "BAM").get();
        ENAHtsgetTicket expectedTicket = new ENAHtsgetTicket();
        expectedTicket.setAccession("SAMN07666497");
        expectedTicket.setFormat("BAM");
        expectedTicket.setMd5Hash("bd1a36383f9b5b7ff5a8df79e85de245");
        expectedTicket.addFtpLink("localhost:8086/sample?accession=SAMN07666497&format=BAM&part=1");
        assertTrue(ticket.equals(expectedTicket));
    }

    @Test
    public void cramTicketTest() {
        createExpectationForCramFile();
        ENAHtsgetTicket ticket = htsgetService.getTicket("SAMN07666497", "CRAM").get();
        ENAHtsgetTicket expectedTicket = new ENAHtsgetTicket();
        expectedTicket.setAccession("SAMN07666497");
        expectedTicket.setFormat("CRAM");
        expectedTicket.setMd5Hash("bd1a36383f9b5b7ff5a8df79e85de245");
        expectedTicket.addFtpLink("localhost:8086/sample?accession=SAMN07666497&format=CRAM&part=1");
        assertEquals(ticket, expectedTicket);

    }
}