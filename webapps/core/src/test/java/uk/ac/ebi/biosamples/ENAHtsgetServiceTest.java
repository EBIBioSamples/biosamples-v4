package uk.ac.ebi.biosamples;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.biosamples.model.ENAHtsgetTicket;
import uk.ac.ebi.biosamples.service.ENAHtsgetService;

import static org.junit.Assert.*;

public class ENAHtsgetServiceTest {

    @Autowired
    ENAHtsgetService htsgetService;

    public ENAHtsgetServiceTest(){
        htsgetService = new ENAHtsgetService();
    }
    @Test
    public void bamTicketTest(){
        ENAHtsgetTicket ticket = htsgetService.getTicket("SAMN07666497", "BAM");
        ENAHtsgetTicket expectedTicket = new ENAHtsgetTicket();
        expectedTicket.setAccession("SAMN07666497");
        expectedTicket.setFormat("BAM");
        expectedTicket.setMd5Hash("bd1a36383f9b5b7ff5a8df79e85de245");
        expectedTicket.addFtpLink("localhost:8080/sample?accession=SAMN07666497&format=BAM&part=1");
        assertTrue(ticket.equals(expectedTicket));
    }

    @Test
    public void cramTicketTest(){
        ENAHtsgetTicket ticket = htsgetService.getTicket("SAMN07666497", "CRAM");
        ENAHtsgetTicket expectedTicket = new ENAHtsgetTicket();
        expectedTicket.setAccession("SAMN07666497");
        expectedTicket.setFormat("CRAM");
        expectedTicket.setMd5Hash("bd1a36383f9b5b7ff5a8df79e85de245");
        expectedTicket.addFtpLink("localhost:8080/sample?accession=SAMN07666497&format=CRAM&part=1");
        assertEquals(ticket, expectedTicket);

    }
}