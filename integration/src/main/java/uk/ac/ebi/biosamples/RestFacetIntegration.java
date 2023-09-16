/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples;

import java.time.Instant;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

@Component
@Order(3)
// @Profile({"default","rest"})
public class RestFacetIntegration extends AbstractIntegration {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final IntegrationProperties integrationProperties;
  private final BioSamplesProperties bioSamplesProperties;

  public RestFacetIntegration(
      final BioSamplesClient client,
      final IntegrationProperties integrationProperties,
      final BioSamplesProperties bioSamplesProperties) {
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
    EntityModel<Sample> resource = client.persistSampleResource(sampleTest1);

    final Attribute sraAccessionAttribute1 =
        resource.getContent().getAttributes().stream()
            .filter(attribute -> attribute.getType().equals("SRA accession"))
            .findFirst()
            .get();

    sampleTest1.getAttributes().add(sraAccessionAttribute1);
    sampleTest1 =
        Sample.Builder.fromSample(sampleTest1)
            .withAccession(resource.getContent().getAccession())
            .build();
    if (!sampleTest1.equals(resource.getContent())) {
      throw new IntegrationTestFailException(
          "Expected response ("
              + resource.getContent()
              + ") to equal submission ("
              + sampleTest1
              + ")",
          Phase.ONE);
    }

    resource = client.persistSampleResource(enaSampleTest);

    final Attribute sraAccessionAttribute2 =
        resource.getContent().getAttributes().stream()
            .filter(attribute -> attribute.getType().equals("SRA accession"))
            .findFirst()
            .get();
    enaSampleTest.getAttributes().add(sraAccessionAttribute2);
    enaSampleTest =
        Sample.Builder.fromSample(enaSampleTest)
            .withAccession(resource.getContent().getAccession())
            .build();
    if (!enaSampleTest.equals(resource.getContent())) {
      throw new IntegrationTestFailException(
          "Expected response ("
              + resource.getContent()
              + ") to equal submission ("
              + enaSampleTest
              + ")",
          Phase.ONE);
    }

    resource = client.persistSampleResource(aeSampleTest);

    final Attribute sraAccessionAttribute3 =
        resource.getContent().getAttributes().stream()
            .filter(attribute -> attribute.getType().equals("SRA accession"))
            .findFirst()
            .get();
    aeSampleTest.getAttributes().add(sraAccessionAttribute3);
    aeSampleTest =
        Sample.Builder.fromSample(aeSampleTest)
            .withAccession(resource.getContent().getAccession())
            .build();
    if (!aeSampleTest.equals(resource.getContent())) {
      throw new IntegrationTestFailException(
          "Expected response ("
              + resource.getContent()
              + ") to equal submission ("
              + aeSampleTest
              + ")",
          Phase.ONE);
    }
  }

  @Override
  protected void phaseTwo() {
    /*
     * disable untill we can properly implement a facet format
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
    */
    // TODO check that the particular facets we expect are present

  }

  @Override
  protected void phaseThree() {

    /*
     * disable untill we can properly implement a facet format
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
    */
  }

  @Override
  protected void phaseFour() {}

  @Override
  protected void phaseFive() {}

  @Override
  protected void phaseSix() {}

  private Sample getSampleTest1() {
    final String name = "RestFacetIntegration_testRestFacet";
    final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

    final SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    // use non alphanumeric characters in type
    attributes.add(Attribute.build("geographic location (country and/or sea)", "Land of Oz"));

    return new Sample.Builder(name)
        .withDomain(defaultIntegrationSubmissionDomain)
        .withRelease(release)
        .withUpdate(update)
        .withAttributes(attributes)
        .build();
  }

  private Sample getEnaSampleTest() {
    final String name = "RestFacetIntegration_testEnaRestFacet";
    final Instant update = Instant.parse("2015-03-22T08:30:23.00Z");
    final Instant release = Instant.parse("2015-03-22T08:30:23.00Z");

    final SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    // use non alphanumeric characters in type
    attributes.add(Attribute.build("geographic location (country and/or sea)", "Land of Oz"));

    final SortedSet<ExternalReference> externalReferences = new TreeSet<>();
    externalReferences.add(
        ExternalReference.build(
            "https://www.ebi.ac.uk/ena/ERA123123",
            new TreeSet<>(Arrays.asList("DUO:0000005", "DUO:0000001", "DUO:0000007"))));
    externalReferences.add(
        ExternalReference.build("http://www.ebi.ac.uk/arrayexpress/experiments/E-MTAB-09123"));

    return new Sample.Builder(name)
        .withDomain(defaultIntegrationSubmissionDomain)
        .withRelease(release)
        .withUpdate(update)
        .withAttributes(attributes)
        .withExternalReferences(externalReferences)
        .build();
  }

  private Sample getArrayExpressSampleTest() {
    final String name = "RestFacetIntegration_testArrayExpressRestFacet";
    final Instant update = Instant.parse("2015-03-22T08:30:23.00Z");
    final Instant release = Instant.parse("2015-03-22T08:30:23.00Z");

    final SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    // use non alphanumeric characters in type
    attributes.add(Attribute.build("geographic location (country and/or sea)", "Land of Oz"));

    final SortedSet<ExternalReference> externalReferences = new TreeSet<>();
    externalReferences.add(
        ExternalReference.build("http://www.ebi.ac.uk/arrayexpress/experiments/E-MTAB-5277"));

    return new Sample.Builder(name)
        .withDomain(defaultIntegrationSubmissionDomain)
        .withRelease(release)
        .withUpdate(update)
        .withAttributes(attributes)
        .withExternalReferences(externalReferences)
        .build();
  }
}
