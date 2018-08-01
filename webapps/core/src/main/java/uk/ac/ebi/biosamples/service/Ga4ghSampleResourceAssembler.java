package uk.ac.ebi.biosamples.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.ENAHtsgetTicket;
import uk.ac.ebi.biosamples.model.ga4gh.AttributeValue;
import uk.ac.ebi.biosamples.model.ga4gh.Attributes;
import uk.ac.ebi.biosamples.model.ga4gh.Ga4ghSample;

import java.util.*;

@Service
public class Ga4ghSampleResourceAssembler implements ResourceAssembler<Ga4ghSample, Resource<Ga4ghSample>> {

    private ENAHtsgetService htsgetService;

    @Autowired
    public Ga4ghSampleResourceAssembler(ENAHtsgetService htsgetService) {
        this.htsgetService = htsgetService;
    }

    @Override
    public Resource<Ga4ghSample> toResource(Ga4ghSample ga4ghSample) {
        Resource<Ga4ghSample> resource = new Resource<>(ga4ghSample);
        List<String> accessions = getAccessionFromEnaLink(ga4ghSample);
        Set<Link> bamFiles = new LinkedHashSet<>();
        Set<Link> cramFiles = new LinkedHashSet<>();
        for (String accession : accessions) {
            bamFiles.addAll(getLinksToFiles(accession, "BAM"));
            cramFiles.addAll(getLinksToFiles(accession, "CRAM"));
        }
        resource.add(new ArrayList<>(bamFiles));
        resource.add(new ArrayList<>(cramFiles));
        return resource;
    }

    public List<Link> getLinksToFiles(String accession, String format) {
        List<Link> links = new ArrayList();
        Optional<ENAHtsgetTicket> ticket = htsgetService.getTicket(accession, format);
        if (ticket.isPresent()) {
            List<String> urls = ticket.get().getFtpLinks();
            for (String url : urls) {
                links.add(new Link(url, format + "_files"));
            }
        }
        return links;
    }

    private List<String> getAccessionFromEnaLink(Ga4ghSample ga4ghSample) {
        List<String> accessions = new ArrayList<>();
        List<AttributeValue> externalReferences = ga4ghSample.getAttributes().getAttributes().get("external_references");
        for (AttributeValue value : externalReferences) {
            Attributes urlAttribute = (Attributes) value.getValue();
            String url = (String) urlAttribute.getAttributes().get("url").get(0).getValue();
            String[] elements = url.split("/");
            accessions.add(elements[elements.length - 1]);
        }
        return accessions;
    }
}
