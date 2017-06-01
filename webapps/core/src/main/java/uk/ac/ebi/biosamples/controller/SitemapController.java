package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.expression.ParseException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.service.SamplePageService;
import uk.ac.ebi.biosamples.service.SampleService;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.net.MalformedURLException;

@Controller
@RequestMapping("/sitemap")
public class SitemapController {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${sitemap.page.size:10000}")
    private int sitemapPageSize;

    private SampleService sampleService;
    private SamplePageService samplePageService;

    public SitemapController(SampleService service, SamplePageService pageService) {
        this.sampleService = service;
        this.samplePageService = pageService;
    }

    @RequestMapping(method= RequestMethod.GET, produces= MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String createSampleSitemapIndex(HttpServletRequest request) throws MalformedURLException {

        long sampleCount = getTotalSamples();
        long pageNumber = Math.floorDiv(sampleCount,sitemapPageSize) + 1L;
        XmlSitemapIndex xmlSitemapIndex = new XmlSitemapIndex();
        String requestUrl = request.getRequestURL().toString().replaceFirst(request.getRequestURI(), "");
        for (int i=0; i< pageNumber; i++) {
            String location = generateBaseUrl(request) + String.format("/sitemap/%d", i+1);
            XmlSitemap xmlSiteMap = new XmlSitemap(location);
            xmlSitemapIndex.addSitemap(xmlSiteMap);
        }
        return getSitemapFile(xmlSitemapIndex);

    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String createSampleSitemapPage(@PathVariable("id") int pageNumber, HttpServletRequest request) throws ParseException {
        final long startTime = System.currentTimeMillis();
        Pageable pageRequest = new PageRequest(pageNumber - 1, sitemapPageSize);
        Page<Sample> samplePage = samplePageService.getSamplesByText("", null, pageRequest);
        XmlUrlSet xmlUrlSet = new XmlUrlSet();
        for(Sample sample: samplePage.getContent()) {
            String location = generateBaseUrl(request) + String.format("/samples/%s", sample.getAccession());
            XmlUrl url = new XmlUrl.XmlUrlBuilder(location)
                    .lastModified(sample.getUpdate().toLocalDate())
                    .hasPriority(XmlUrl.Priority.MEDIUM).build();
            xmlUrlSet.addUrl(url);
        }
        log.debug(String.format("Returning sitemap for %d samples took %d millis", sitemapPageSize, System.currentTimeMillis() - startTime));
        return getSitemapFile(xmlUrlSet);
    }

    private String getSitemapFile(Object xmlObject) {
        StringWriter writer = new StringWriter(2048);

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(xmlObject.getClass());
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            // Set the XML root tag to not include that standalone="yes" attribute
            // Why U no work, JAXB? :O//
            //jaxbMarshaller.setProperty("com.sun.xml.bind.xmlHeaders", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

            jaxbMarshaller.marshal(xmlObject, writer);

        } catch (JAXBException e) {
            e.printStackTrace();
        }

        return writer.toString();

//        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + writer.toString();
    }

    private String generateBaseUrl(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String requestURL = request.getRequestURL().toString();
        return requestURL.replaceFirst(requestURI, "") +
                request.getContextPath();

    }
    private long getTotalSamples() {
        Pageable pageable = new PageRequest(0, 1);
        MultiValueMap<String, String> filters = new LinkedMultiValueMap<>();
        Page<Sample> samplePage = samplePageService.getSamplesByText("", filters, pageable);
        return samplePage.getTotalElements();
    }
}
