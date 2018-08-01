package uk.ac.ebi.biosamples;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import uk.ac.ebi.biosamples.model.ENAHtsgetTicket;
import uk.ac.ebi.biosamples.model.ga4gh.AttributeValue;
import uk.ac.ebi.biosamples.model.ga4gh.Ga4ghAttributes;
import uk.ac.ebi.biosamples.model.ga4gh.Ga4ghSample;
import uk.ac.ebi.biosamples.service.ENAHtsgetService;
import uk.ac.ebi.biosamples.service.Ga4ghSampleResourceAssembler;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
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

    public Ga4ghSampleResourceAssemblerUnitTest() {
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
        for (String link : bamTicket.getFtpLinks()) {
            linksFromHtsget.add(new Link(link, "BAM_files"));

        }

        for (String link : cramTicket.getFtpLinks()) {
            linksFromHtsget.add(new Link(link, "CRAM_files"));
        }

        assertEquals(linksFromResource, linksFromHtsget);
    }

    void setUpMocks() {
        MockitoAnnotations.initMocks(this);
        when(sample.getId()).thenReturn("accession");
        when(sample.getDescription()).thenReturn("This is test sample");
        Ga4ghAttributes externalAttributes = mock(Ga4ghAttributes.class);
        SortedMap<String, List<AttributeValue>> attributes = mock(SortedMap.class);
        List<AttributeValue> values = new ArrayList<>();
        AttributeValue urlAttribute = mock(AttributeValue.class);
        Ga4ghAttributes urlAttributes = mock(Ga4ghAttributes.class);
        SortedMap<String, List<AttributeValue>> urlMap = new TreeMap<>();
        List<AttributeValue> urlList = new ArrayList<>();
        AttributeValue urlValue = mock(AttributeValue.class);
        when(sample.getAttributes()).thenReturn(externalAttributes);
        when(externalAttributes.getAttributes()).thenReturn(attributes);
        when(attributes.get("external_references")).thenReturn(values);
        values.add(urlAttribute);
        when(urlAttribute.getValue()).thenReturn(urlAttributes);
        when(urlAttributes.getAttributes()).thenReturn(urlMap);
        when(urlValue.getValue()).thenReturn("http://www.ebi.ac.uk/ena/data/view/accession");
        urlList.add(urlValue);
        urlMap.put("url", urlList);
        when(bamTicket.getAccession()).thenReturn("accession");
        when(bamTicket.getFormat()).thenReturn("BAM");
        when(bamTicket.getFtpLinks()).thenReturn(Lists.newArrayList("testlink1"));

        when(cramTicket.getAccession()).thenReturn("accession");
        when(cramTicket.getFormat()).thenReturn("CRAM");
        when(cramTicket.getFtpLinks()).thenReturn(Lists.newArrayList("testlink2"));

        when(htsgetService.getTicket("accession", "BAM")).thenReturn(Optional.of(bamTicket));
        when(htsgetService.getTicket("accession", "CRAM")).thenReturn(Optional.of(cramTicket));

    }

}