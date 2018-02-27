package uk.ac.ebi.biosamples;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.XmlSitemap;
import uk.ac.ebi.biosamples.model.XmlSitemapIndex;
import uk.ac.ebi.biosamples.model.XmlUrlSet;

@Order
@Component
@Profile({"default"})
public class SitemapIntegration extends AbstractIntegration {

    private URI biosamplesSubmissionUri;

    private final RestOperations restTemplate;
    @Value("${model.page.size:10000}")
    private int sitemapPageSize;

    public SitemapIntegration(BioSamplesClient client,
                              RestTemplateBuilder restTemplateBuilder,
                              BioSamplesProperties bioSamplesProperties) {
        super(client);
        this.biosamplesSubmissionUri = bioSamplesProperties.getBiosamplesClientUri();
        this.restTemplate = restTemplateBuilder.build();

    }

    @Override
    protected void phaseOne() {

    }

    @Override
    protected void phaseTwo() {
        List<Resource<Sample>> samples = new ArrayList<>();
        Map<String, Boolean> lookupTable = new HashMap<>();
        for (Resource<Sample> sample : client.fetchSampleResourceAll()) {
            samples.add(sample);
            lookupTable.put(sample.getContent().getAccession(), Boolean.FALSE);
        }

        if (samples.size() <= 0) {
            throw new RuntimeException("No search results found!");
        }

        int expectedSitemapIndexSize = Math.floorDiv(samples.size(),sitemapPageSize) + 1;

        XmlSitemapIndex index = getSitemapIndex();
        if (index.getXmlSitemaps().size() != expectedSitemapIndexSize) {
            throw new RuntimeException("The model index size ("+index.getXmlSitemaps().size()+") doesn't match the expected size ("+expectedSitemapIndexSize+")");
        }

        for (XmlSitemap sitemap : index.getXmlSitemaps()) {
            XmlUrlSet urlSet = getUrlSet(sitemap);
            urlSet.getXmlUrls().forEach(xmlUrl -> {
                UriComponents sampleUri = UriComponentsBuilder.fromPath(xmlUrl.getLoc()).build();
                String sampleAccession = getAccessionFromUri(sampleUri);
                lookupTable.replace(sampleAccession, Boolean.TRUE);
            });
        }
        lookupTable.entrySet().stream().filter(entry -> !entry.getValue()).findFirst().ifPresent(entry -> {
            throw new RuntimeException("Sample "+entry.getKey()+" is not in the sitemap");
        });

    }

    @Override
    protected void phaseThree() {

    }

    @Override
    protected void phaseFour() {

    }

    @Override
    protected void phaseFive() {

    }

    private String getAccessionFromUri(UriComponents uri) {
        List<String> pathSegments = uri.getPathSegments();
        return pathSegments.get(pathSegments.size() - 1);
    }

    private XmlSitemapIndex getSitemapIndex() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(biosamplesSubmissionUri);
        UriComponents sitemapUri = builder.pathSegment("sitemap").build();
        ResponseEntity<XmlSitemapIndex> responseEntity = restTemplate.getForEntity(sitemapUri.toUri(), XmlSitemapIndex.class);
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Sitemap not available");
        }
        return responseEntity.getBody();

    }

    private XmlUrlSet getUrlSet(XmlSitemap sitemap) {
        ResponseEntity<XmlUrlSet> urlSetReponseEntity = restTemplate.getForEntity(sitemap.getLoc(), XmlUrlSet.class);
        if (!urlSetReponseEntity.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Unable to reach a model urlset");
        }
        return urlSetReponseEntity.getBody();

    }
}
