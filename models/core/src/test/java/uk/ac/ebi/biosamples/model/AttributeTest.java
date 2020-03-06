package uk.ac.ebi.biosamples.model;

import org.junit.Test;
import uk.ac.ebi.biosamples.service.HttpOlsUrlResolutionService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

//@RunWith(SpringRunner.class)
//@JsonTest
public class AttributeTest {

	@Test
	public void test_getIriOls_method_returns_null_if_invalid_iri_is_provided() {
		Attribute testAttribute = Attribute.build(
				"WrongIRIAttributeKey",
				"WrongIRIAttributeValue",
				"Something else",
				null
		);

		HttpOlsUrlResolutionService httpOlsUrlResolutionService = new HttpOlsUrlResolutionService();

		assertThat(httpOlsUrlResolutionService.getIriOls(testAttribute.getIri()), nullValue());
	}

	@Test
	public void test_getIriOls_method_returns_iri_if_valid_iri_is_provided() throws UnsupportedEncodingException {
		String iri = "http://purl.obolibrary.org/obo/NCBITaxon_291302";
		Attribute testAttribute = Attribute.build(
				"Organism",
				"Miniopterus natalensis",
				iri,
				null
		);

		HttpOlsUrlResolutionService httpOlsUrlResolutionService = new HttpOlsUrlResolutionService();

		System.out.println(httpOlsUrlResolutionService.getIriOls(testAttribute.getIri()));
		assertThat(httpOlsUrlResolutionService.getIriOls(testAttribute.getIri()), allOf(
				endsWith("NCBITaxon_291302"),
				startsWith("http://www.ebi.ac.uk/ols/terms?iri="),
				containsString(URLEncoder.encode(iri, StandardCharsets.UTF_8))));
	}

	@Test
	public void test_getIriOls_method_returns_null_for_a_query() {
		String curie = "CL:0000451";
		Attribute testAttribute = Attribute.build(
				"Cell type",
				"dendritic cell",
				curie,
				null
		);

		HttpOlsUrlResolutionService httpOlsUrlResolutionService = new HttpOlsUrlResolutionService();

		assertThat(httpOlsUrlResolutionService.getIriOls(testAttribute.getIri()), nullValue());
	}
}
