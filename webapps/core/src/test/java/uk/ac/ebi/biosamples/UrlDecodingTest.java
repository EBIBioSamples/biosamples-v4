package uk.ac.ebi.biosamples;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.util.UriUtils;

import uk.ac.ebi.biosamples.utils.LinkUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.junit.Assert;

@RunWith(SpringRunner.class)
public class UrlDecodingTest {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Test
	public void testSpringUriUtils() throws UnsupportedEncodingException {
		Assert.assertEquals("AoErVGVzdEZpbHRlcjM=", UriUtils.decode("AoErVGVzdEZpbHRlcjM%3D", "UTF-8"));
		Assert.assertEquals("AoErVGVzdEZpbHRlcjM=", UriUtils.decode("AoErVGVzdEZpbHRlcjM=", "UTF-8"));
	}
	@Test
	public void testJavaUrlDecoder() throws UnsupportedEncodingException {
		Assert.assertEquals("AoErVGVzdEZpbHRlcjM=", URLDecoder.decode("AoErVGVzdEZpbHRlcjM%3D", "UTF-8"));
		Assert.assertEquals("AoErVGVzdEZpbHRlcjM=", URLDecoder.decode("AoErVGVzdEZpbHRlcjM=", "UTF-8"));
	}
/*	
 * this would be expected to work, but fails
	@Test
	public void testWebUtilUriTemplate() {
		String uriTemplated = "http://localhost:8081/biosamples/samples?text=&cursor=AoErVGVzdEZpbHRlcjM%3D&size=1000{&filter,page,sort}";
		String uriUntemplated = "http://localhost:8081/biosamples/samples?text=&cursor=AoErVGVzdEZpbHRlcjM%3D&size=1000";
		Assert.assertEquals(uriUntemplated,	new org.springframework.web.util.UriTemplate(uriTemplated).expand().toString());
	}
*/	
	
/*	
 * this would be expected to work, but fails
	@Test
	public void testHateoasUriTemplate() {
		String uriTemplated = "http://localhost:8081/biosamples/samples?text=&cursor=AoErVGVzdEZpbHRlcjM%3D&size=1000{&filter,page,sort}";
		String uriUntemplated = "http://localhost:8081/biosamples/samples?text=&cursor=AoErVGVzdEZpbHRlcjM%3D&size=1000";
		Assert.assertEquals(uriUntemplated,	new org.springframework.hateoas.UriTemplate(uriTemplated).expand().toString());
	}
*/	
	
	@Test
	public void testStrippedHateoasUriTemplate() {
		String uriTemplated = "http://localhost:8081/biosamples/samples?text=&cursor=AoErVGVzdEZpbHRlcjM%3D&size=1000{&filter,page,sort}";
		String uriUntemplated = "http://localhost:8081/biosamples/samples?text=&cursor=AoErVGVzdEZpbHRlcjM%3D&size=1000";
		Assert.assertEquals(uriUntemplated,	new org.springframework.hateoas.UriTemplate(uriTemplated.replaceAll("\\{.*\\}", "")).expand().toString());
	}
}
