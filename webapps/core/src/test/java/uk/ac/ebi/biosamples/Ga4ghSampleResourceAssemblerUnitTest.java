package uk.ac.ebi.biosamples;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import uk.ac.ebi.biosamples.model.ENAHtsgetTicket;
import uk.ac.ebi.biosamples.model.ga4gh.Ga4ghSample;
import uk.ac.ebi.biosamples.service.ENAHtsgetService;
import uk.ac.ebi.biosamples.service.Ga4ghSampleResourceAssembler;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class Ga4ghSampleResourceAssemblerUnitTest {

    @Mock
    Ga4ghSample sample;
    @Mock
    ENAHtsgetService htsgetService;
    @Mock
    ENAHtsgetTicket bamTicket;
    @Mock
    ENAHtsgetTicket cramTicket;

    Ga4ghSampleResourceAssembler assembler;

    public Ga4ghSampleResourceAssemblerUnitTest(){
        setUpMocks();
        assembler = new Ga4ghSampleResourceAssembler(htsgetService);
    }

    @Test
    public void assemblerTest() {
        ObjectMapper mapper = new ObjectMapper();
        Resource<Ga4ghSample> resource = assembler.toResource(sample);
        Ga4ghSample sampleFromResource = resource.getContent();
        List<Link> linksFromResource = resource.getLinks();

        assertTrue(sample.equals(sampleFromResource));

        List<Link> linksFromHtsget = new ArrayList<>();
        Integer i =1;
        for(String link:bamTicket.getFtpLinks()){
            linksFromHtsget.add(new Link(link,"BAMLink_"+i.toString() ));
            i++;
        }
        i =1;
        for(String link:cramTicket.getFtpLinks()){
            linksFromHtsget.add(new Link(link,"CRAMLink_"+i.toString() ));
            i++;
        }

        assertEquals(linksFromResource,linksFromHtsget);
    }

    void setUpMocks(){
        MockitoAnnotations.initMocks(this);

        when(sample.getId()).thenReturn("accession");
        when(sample.getDescription()).thenReturn("This is test sample");

        when(bamTicket.getAccession()).thenReturn("accession");
        when(bamTicket.getFormat()).thenReturn("BAM");
        when(bamTicket.getFtpLinks()).thenReturn(Lists.newArrayList("testlink1"));

        when(cramTicket.getAccession()).thenReturn("accession");
        when(cramTicket.getFormat()).thenReturn("CRAM");
        when(cramTicket.getFtpLinks()).thenReturn(Lists.newArrayList("testlink2"));


        when(htsgetService.getTicket("accession","BAM")).thenReturn(Optional.of(bamTicket));
        when(htsgetService.getTicket("accession","CRAM")).thenReturn(Optional.of(cramTicket));

    }

}