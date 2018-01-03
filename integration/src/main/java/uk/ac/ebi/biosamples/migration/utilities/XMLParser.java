package uk.ac.ebi.biosamples.migration.utilities;

import java.io.StringReader;
import java.net.URI;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.XmlGroupToSampleConverter;
import uk.ac.ebi.biosamples.service.XmlSampleToSampleConverter;

public class XMLParser implements BioSampleApiParser{

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String baseUri;
    private final RestTemplate restTemplate;

    public XMLParser(RestTemplate restTemplate, String baseUri) {
        this.baseUri = baseUri;
        this.restTemplate = restTemplate;
    }

    @Override
    public Sample getSample(String accession) {
        UriComponentsBuilder oldUriComponentBuilder = UriComponentsBuilder.fromUriString(baseUri);
        URI sampleURI = oldUriComponentBuilder.cloneBuilder().pathSegment(accession).build().toUri();
        String document = getDocument(sampleURI);
        SAXReader saxReader = new SAXReader();
        org.dom4j.Document doc;
        try {
            doc = saxReader.read(new StringReader(document));
        } catch (DocumentException e) {
            throw new HttpMessageNotReadableException("error parsing xml", e);
        }

        if (accession.startsWith("SAMEG"))
            return convertXMLGroup(doc.getRootElement());
        else
            return convertXMLSample(doc.getRootElement());
    }

    public String getDocument(URI uri) {
        //log.info("Getting " + uri);
        ResponseEntity<String> response;
        try {
            response = restTemplate.getForEntity(uri, String.class);
        } catch (RestClientException e) {
            log.error("Problem accessing " + uri, e);
            throw e;
        }
        // xmlString = toPrettyString(xmlString, 2);
        return response.getBody();
    }

    private Sample convertXMLGroup(Element groupXML) {
        return new XmlGroupToSampleConverter().convert(groupXML);
    }

    private Sample convertXMLSample(Element sampleXML) {
        return new XmlSampleToSampleConverter().convert(sampleXML);
    }
}
