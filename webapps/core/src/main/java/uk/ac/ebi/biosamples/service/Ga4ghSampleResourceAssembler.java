package uk.ac.ebi.biosamples.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.ENAHtsgetTicket;
import uk.ac.ebi.biosamples.model.ga4gh.Ga4ghSample;

import java.util.ArrayList;
import java.util.List;
@Service
public class Ga4ghSampleResourceAssembler implements ResourceAssembler<Ga4ghSample,Resource<Ga4ghSample>> {

    private ENAHtsgetService htsgetService;

    @Autowired
    public Ga4ghSampleResourceAssembler(ENAHtsgetService htsgetService){
        this.htsgetService = htsgetService;
    }

    @Override
    public Resource<Ga4ghSample> toResource(Ga4ghSample ga4ghSample) {
        Resource<Ga4ghSample> resource = new Resource<>(ga4ghSample);
        resource.add(getLinksToFiles(ga4ghSample.getId(),"BAM"));
        resource.add(getLinksToFiles(ga4ghSample.getId(),"CRAM"));
        return resource;
    }

    public List<Link> getLinksToFiles(String accession,String format){
        List<Link> links = new ArrayList();
        ENAHtsgetTicket ticket = htsgetService.getTicket(accession,format);
        List<String> urls = ticket.getFtpLinks();
        Integer i = 1;
        for(String url: urls){
            links.add(new Link(url,format+"Link_"+i.toString() ));
            i++;
        }
        return links;
    }
}
