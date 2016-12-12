package uk.ac.ebi.biosamples.utils;

import java.net.URI;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class HateoasUtilsTest {

	private MockRestServiceServer server;
	
	private HateoasUtils hateoasUtils;

	@Before
	public void setup() {
				
		MediaType mediaHalJson = MediaType.parseMediaType("application/hal+json");
		List<MediaType> mediasHalJson = MediaType.parseMediaTypes("application/hal+json");
		
		RestTemplate restTemplate = new RestTemplate();	
		
		
		//need to create a new message converter to handle hal+json
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.registerModule(new Jackson2HalModule());
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setSupportedMediaTypes(mediasHalJson);
		converter.setObjectMapper(mapper);

		//add the new converters to the restTemplate
		//but make sure it is BEFORE the existing converters
		List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
		converters.add(0,converter);
		restTemplate.setMessageConverters(converters);
		
		
		server = MockRestServiceServer.bindTo(restTemplate).build();

		server.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo("/api"))
			.andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
			.andRespond(MockRestResponseCreators.withSuccess("{ \"_links\" : { \"this\" : { \"href\" : \"/api/this\" } } }", mediaHalJson));

		server.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo("/api/this"))
			.andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
			.andRespond(MockRestResponseCreators.withSuccess("{ \"_links\" : { \"is\" : { \"href\" : \"/api/this/is\" } } }", mediaHalJson));

		server.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo("/api/this/is"))
			.andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
			.andRespond(MockRestResponseCreators.withSuccess("{ \"_links\" : { \"a\" : { \"href\" : \"/api/this/is/a\" } } }", mediaHalJson));

		server.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo("/api/this/is/a"))
			.andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
			.andRespond(MockRestResponseCreators.withSuccess("{ \"_links\" : { \"test\" : { \"href\" : \"/api/this/is/a/test\" } } }", mediaHalJson));

		//this response is overloaded to be compatible with both Resource and Resources
		server.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo("/api/this/is/a/test"))
			.andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
			.andRespond(MockRestResponseCreators.withSuccess("{ \"name\" : \"floble\", \"_embedded\" :  { \"things\" : [ { \"name\" : \"floble\", \"_links\" : { \"self\" : { \"href\" : \"/api/this/is/a/test\" } } } ] }, \"_links\" : { \"self\" : { \"href\" : \"/api/this/is/a/test\" } } }", mediaHalJson));
	
		
		hateoasUtils = new HateoasUtils(restTemplate);
	}



	
	@Test
	public void getHateoasUriTemplateTest() {
		UriTemplate template = hateoasUtils.getHateoasUriTemplate(URI.create("/api"),  "this", "is", "a", "test");
	
		Assert.assertNotNull(template);
		Assert.assertEquals("/api/this/is/a/test", template.expand().toString());
		
		// Verify all expectations met
		server.verify();
	}
	
	@Test
	public void getHateoasResourceTest() {
		Resource<Thing> resource = hateoasUtils.getHateoasResource(URI.create("/api"), new ParameterizedTypeReference<Resource<Thing>>(){}, "this", "is", "a", "test");		
		
		Assert.assertNotNull(resource);
		Assert.assertNotNull(resource.getContent());		
		Assert.assertEquals("floble", resource.getContent().getName());
		Assert.assertEquals("/api/this/is/a/test", resource.getLink("self").getHref());
		
		// Verify all expectations met
		server.verify();
	}

	@Test
	public void getHateoasResourcesTest() {
		ResponseEntity<Resources<Resource<Thing>>> response = hateoasUtils.getHateoasResponse(
				URI.create("/api"), new ParameterizedTypeReference<Resources<Resource<Thing>>>(){}, "this", "is", "a", "test");		
		
		Assert.assertNotNull(response);
		Assert.assertNotNull(response.getBody());
		Assert.assertNotNull(response.getBody().getContent());	
		Assert.assertEquals("/api/this/is/a/test", response.getBody().getLink("self").getHref());
		Assert.assertEquals(1, response.getBody().getContent().size());		
		Resource<Thing> thing = response.getBody().getContent().iterator().next();
		Assert.assertEquals("floble", thing.getContent().getName());
		Assert.assertEquals("/api/this/is/a/test", thing.getLink("self").getHref());
		
		// Verify all expectations met
		server.verify();
	}
	
	private static class Thing {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
