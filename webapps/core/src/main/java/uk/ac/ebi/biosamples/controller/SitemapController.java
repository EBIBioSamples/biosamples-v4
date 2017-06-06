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
import java.net.MalformedURLException;

@Controller
@RequestMapping("/sitemap")
public class SitemapController {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${model.page.size:10000}")
    private int sitemapPageSize;

    private SampleService sampleService;
    private SamplePageService samplePageService;

    public SitemapController(SampleService service, SamplePageService pageService) {
        this.sampleService = service;
        this.samplePageService = pageService;
    }

    @RequestMapping(method= RequestMethod.GET, produces= MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public XmlSitemapIndex createSampleSitemapIndex(HttpServletRequest request) throws MalformedURLException {

        long sampleCount = getTotalSamples();
        long pageNumber = Math.floorDiv(sampleCount,sitemapPageSize) + 1L;
        XmlSitemapIndex xmlSitemapIndex = new XmlSitemapIndex();
        for (int i=0; i< pageNumber; i++) {
            String location = generateBaseUrl(request) + String.format("/sitemap/%d", i+1);
            XmlSitemap xmlSiteMap = new XmlSitemap(location);
            xmlSitemapIndex.addSitemap(xmlSiteMap);
        }
        return xmlSitemapIndex;

    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public XmlUrlSet createSampleSitemapPage(@PathVariable("id") int pageNumber, HttpServletRequest request) throws ParseException {
        final long startTime = System.currentTimeMillis();
        Pageable pageRequest = new PageRequest(pageNumber - 1, sitemapPageSize);
        Page<Sample> samplePage = samplePageService.getSamplesByText("", null, null, null, pageRequest);
        XmlUrlSet xmlUrlSet = new XmlUrlSet();
        for(Sample sample: samplePage.getContent()) {
            String location = generateBaseUrl(request) + String.format("/samples/%s", sample.getAccession());
            XmlUrl url = new XmlUrl.XmlUrlBuilder(location)
                    .lastModified(sample.getUpdate().toLocalDate())
                    .hasPriority(XmlUrl.Priority.MEDIUM).build();
            xmlUrlSet.addUrl(url);
        }
        log.debug(String.format("Returning model for %d samples took %d millis", sitemapPageSize, System.currentTimeMillis() - startTime));
        return xmlUrlSet;
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
        Page<Sample> samplePage = samplePageService.getSamplesByText("", filters, null, null, pageable);
        return samplePage.getTotalElements();
    }
}
