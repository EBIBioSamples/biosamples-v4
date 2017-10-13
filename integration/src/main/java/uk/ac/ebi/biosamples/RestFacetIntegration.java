package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.client.Traverson;
import org.springframework.hateoas.mvc.TypeReferences;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.facets.Facet;
import uk.ac.ebi.biosamples.model.facets.FacetContent;
import uk.ac.ebi.biosamples.model.facets.LabelCountListContent;

import java.time.Instant;
import java.util.*;

@Component
@Order(3)
@Profile({"default", "test"})
public class RestFacetIntegration extends AbstractIntegration {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	private final IntegrationProperties integrationProperties;
	private final BioSamplesProperties bioSamplesProperties;
	
	public RestFacetIntegration(BioSamplesClient client, IntegrationProperties integrationProperties,
			BioSamplesProperties bioSamplesProperties) {
		super(client);
		this.integrationProperties = integrationProperties;
		this.bioSamplesProperties = bioSamplesProperties;
	}

	@Override
	protected void phaseOne() {
		Sample sampleTest1 = getSampleTest1();		
		// put a sample
		Resource<Sample> resource = client.persistSampleResource(sampleTest1);
		if (!sampleTest1.equals(resource.getContent())) {
			throw new RuntimeException("Expected response to equal submission");
		}
	}

	@Override
	protected void phaseTwo() {

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("text","TESTrestfacet1");
		Traverson traverson = new Traverson(bioSamplesProperties.getBiosamplesClientUri(), MediaTypes.HAL_JSON);
		Traverson.TraversalBuilder builder = traverson.follow("samples", "facet").withTemplateParameters(parameters);
		Resources<Facet> facets = builder.toObject(new TypeReferences.ResourcesType<Facet>(){});

		log.info("GETting from " + builder.asLink().expand(parameters).getHref());

		if (facets.getContent().size() <= 0) {
			throw new RuntimeException("No facets found!");
		}
		List<Facet> content = new ArrayList<>(facets.getContent());
		FacetContent facetContent = content.get(0).getContent();
		if (facetContent instanceof LabelCountListContent) {
			if (((LabelCountListContent) facetContent).size() <= 0) {
				throw new RuntimeException("No facet values found!");
			}

		}

		//check that the particular facets we expect are present

		//TODO reintroduce this part of code
		boolean facetIsCorrect = false;
		for (Facet facet : content) {
		    switch (facet.getLabel()) {
		    	case "organism":
		    		facetIsCorrect = true;
		    		break;
				case "geographic location (country and/or sea)":
					facetIsCorrect = true;
					break;
				default:
					facetIsCorrect = false;
			}
//			if (facet.getLabel().equals("(Attribute) geographic location (country and/or sea)")) {
//				found = true;
//				//check that it has one value that is expected
//				if (facet.getValue().size() != 1) {
//					throw new RuntimeException("More than one facet value for \"geographic location (country and/or sea)\"");
//				}
//				if (!facet.getValue().iterator().next().equals("Land of Oz")) {
//					throw new RuntimeException("Facet value for \"geographic location (country and/or sea)\" was not \"Land of Oz\"");
//				}
//			}
		}
		if (!facetIsCorrect) {
			throw new RuntimeException("Unable to find facet \"(Attribute) geographic location (country and/or sea)\"");
		}
		
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

	private Sample getSampleTest1() {
		String name = "Test Sample";
		String accession = "TESTrestfacet1";
		String domain = "self.BiosampleIntegrationTest";
		Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
		Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
			Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
		//use non alphanumeric characters in type
		attributes.add(Attribute.build("geographic location (country and/or sea)", "Land of Oz", null, null));

		return Sample.build(name, accession, domain, release, update, attributes, null, null);
	}
	
}
