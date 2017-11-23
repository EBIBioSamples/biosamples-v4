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
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.facet.ExternalReferenceDataFacet;
import uk.ac.ebi.biosamples.model.facet.Facet;
import uk.ac.ebi.biosamples.model.facet.FacetType;
import uk.ac.ebi.biosamples.model.facet.content.FacetContent;
import uk.ac.ebi.biosamples.model.facet.content.LabelCountEntry;
import uk.ac.ebi.biosamples.model.facet.content.LabelCountListContent;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Order(3)
@Profile({"default", "rest"})
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
		Sample enaSampleTest = getEnaSampleTest();
		Sample aeSampleTest = getArrayExpressSampleTest();
		// put a sample
		Resource<Sample> resource = client.persistSampleResource(sampleTest1);
		if (!sampleTest1.equals(resource.getContent())) {
			throw new RuntimeException("Expected response to equal submission");
		}

		resource = client.persistSampleResource(enaSampleTest);
		if (!enaSampleTest.equals(resource.getContent())) {
			throw new RuntimeException("Expected response to equal submission");
		}

		resource = client.persistSampleResource(aeSampleTest);
		if (!aeSampleTest.equals(resource.getContent())) {
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
		}
		if (!facetIsCorrect) {
			throw new RuntimeException("Unable to find facet \"(Attribute) geographic location (country and/or sea)\"");
		}
		
	}

	@Override
	protected void phaseThree() {

		Sample enaSample = getEnaSampleTest();
		SortedSet<ExternalReference> sampleExternalRefs = enaSample.getExternalReferences();

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("text",enaSample.getAccession());
		Traverson traverson = new Traverson(bioSamplesProperties.getBiosamplesClientUri(), MediaTypes.HAL_JSON);
		Traverson.TraversalBuilder builder = traverson.follow("samples", "facet").withTemplateParameters(parameters);
		Resources<Facet> facets = builder.toObject(new TypeReferences.ResourcesType<Facet>(){});

		log.info("GETting from " + builder.asLink().expand(parameters).getHref());

		if (facets.getContent().isEmpty()) {
			throw new RuntimeException("Facet endpoint does not contain the expected number of facet");
		}

		List<Facet> facetList = new ArrayList<>(facets.getContent());
		List<ExternalReferenceDataFacet> externalDataFacetList = facets.getContent().stream()
				.filter(facet -> facet.getType().equals(FacetType.EXTERNAL_REFERENCE_DATA_FACET))
				.map(facet -> (ExternalReferenceDataFacet) facet)
				.collect(Collectors.toList());

		for (ExternalReferenceDataFacet facet: externalDataFacetList) {
			List<String> facetContentLabels = facet.getContent().stream().map(LabelCountEntry::getLabel).collect(Collectors.toList());
			for (String extRefDataId: facetContentLabels) {
				boolean found = sampleExternalRefs.stream().anyMatch(extRef -> extRef.getUrl().toLowerCase().endsWith(extRefDataId.toLowerCase()));
				if (!found) {
					throw new RuntimeException("Facet content does not contain expected external reference data id");
				}
			}
		}



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
		attributes.add(Attribute.build("geographic location (country and/or sea)", "Land of Oz"));

		return Sample.build(name, accession, domain, release, update, attributes, null, null, null, null, null);
	}

	private Sample getEnaSampleTest() {
		String name = "Test ENA reference Sample";
		String accession = "TestEnaRestFacet";
		String domain = "self.BiosampleIntegrationTest";
		Instant update = Instant.parse("2015-03-22T08:30:23.00Z");
		Instant release = Instant.parse("2015-03-22T08:30:23.00Z");

		SortedSet<ExternalReference> externalReferences = new TreeSet<>();
		externalReferences.add(ExternalReference.build("https://www.ebi.ac.uk/ena/ERA123123"));
		externalReferences.add(ExternalReference.build("http://www.ebi.ac.uk/arrayexpress/experiments/E-MTAB-09123"));

		return Sample.build(name, accession, domain, release, update, null, null, externalReferences, null, null, null);

	}

	private Sample getArrayExpressSampleTest() {
		String name = "Test ArrayExpress reference Sample";
		String accession = "TestArrayExpressRestFacet";
		String domain = "self.BiosampleIntegrationTest";
		Instant update = Instant.parse("2015-03-22T08:30:23.00Z");
		Instant release = Instant.parse("2015-03-22T08:30:23.00Z");

		SortedSet<ExternalReference> externalReferences = new TreeSet<>();
		externalReferences.add(ExternalReference.build("http://www.ebi.ac.uk/arrayexpress/experiments/E-MTAB-5277"));

		return Sample.build(name, accession, domain, release, update, null, null, externalReferences, null, null, null);

	}

}
