package uk.ac.ebi.biosamples.service;

import java.util.List;
import java.util.Optional;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.controller.SampleCurationLinksRestController;
import uk.ac.ebi.biosamples.controller.SampleRestController;
import uk.ac.ebi.biosamples.model.Sample;

/**
 * This class is used by Spring to add HAL _links for {@Link Sample} objects.
 * 
 * @author faulcon
 *
 */
@Service
public class SampleResourceAssembler implements ResourceAssembler<Sample, Resource<Sample>> {
	
	public static final String REL_CURATIONDOMAIN="curationDomain";
	public static final String REL_CURATIONLINKS="curationLinks";
	public static final String REL_CURATIONLINK="curationLink";
	
	public SampleResourceAssembler() {
	}

	private Link getSelfLink(String accession, String legacydetails, Optional<List<String>> curationDomains) {
    	UriComponentsBuilder uriComponentsBuilder = ControllerLinkBuilder.linkTo(SampleRestController.class, accession).toUriComponentsBuilder();
    	if (legacydetails != null) {
    		uriComponentsBuilder.queryParam("legacydetails", legacydetails);
    	}
    	if (curationDomains.isPresent()) {
    		if (curationDomains.get().size() == 0) {
    			uriComponentsBuilder.queryParam("curationdomain", (Object[])null);
    		} else {
        		for (String curationDomain : curationDomains.get()) {
        			uriComponentsBuilder.queryParam("curationdomain", curationDomain);
        		}
    		}
    	}
    	return new Link(uriComponentsBuilder.build().toUriString(), Link.REL_SELF);
    }
    
    private Link getCurationDomainLink(Link selfLink) {
		UriComponents selfUriComponents = UriComponentsBuilder.fromUriString(selfLink.getHref()).build();
		if (selfUriComponents.getQueryParams().size() == 0) {
			return new Link(selfLink.getHref()+"{?curationdomain}", REL_CURATIONDOMAIN);
		} else {
			return new Link(selfLink.getHref()+"{&curationdomain}", REL_CURATIONDOMAIN);
		}
    }
    
    private Link getCurationLinksLink(String accession) {
    	return ControllerLinkBuilder.linkTo(
			ControllerLinkBuilder.methodOn(SampleCurationLinksRestController.class)
				.getCurationLinkPageJson(accession, null, null)).withRel("curationLinks");
    }
    
    public Resource<Sample> toResource(Sample sample, String legacydetails, Optional<List<String>> curationDomains) {
		Resource<Sample> sampleResource = new Resource<>(sample);
		sampleResource.add(getSelfLink(sample.getAccession(), legacydetails, curationDomains));
		//add link to select curation domain
		sampleResource.add(getCurationDomainLink(sampleResource.getLink(Link.REL_SELF)));				
		//add link to curationLinks on this sample
		sampleResource.add(getCurationLinksLink(sample.getAccession()));
		return sampleResource;
    }
    
	@Override
	public Resource<Sample> toResource(Sample sample) {		
		return toResource(sample, null, null);
	}
    

}