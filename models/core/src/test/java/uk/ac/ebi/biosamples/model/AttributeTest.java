package uk.ac.ebi.biosamples.model;

import org.junit.Test;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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

		assertThat(testAttribute.getIriOls(), nullValue());
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

		assertThat(testAttribute.getIriOls(), allOf(
				endsWith("NCBITaxon_291302"),
				startsWith("http://www.ebi.ac.uk/ols/terms?iri="),
				containsString(URLEncoder.encode(iri, "UTF-8"))));
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

		assertThat(testAttribute.getIriOls(), nullValue());
	}


}
