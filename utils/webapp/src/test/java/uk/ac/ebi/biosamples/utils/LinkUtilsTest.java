package uk.ac.ebi.biosamples.utils;


import java.io.UnsupportedEncodingException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Link;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class LinkUtilsTest {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Test
	public void testCleanLink() {
		Link link = new Link("http://localhost:8080/test/end{?foo,bar}", Link.REL_SELF) ;
		String uri = "http://localhost:8080/test/end";
		Link cleanLink = LinkUtils.cleanLink(link);
		Assert.assertEquals(uri, cleanLink.getHref());
	}
	
	@Test
	public void testCleanLinkEncoded() {
		Link link = new Link("http://localhost:8080/test/end?foo=%21{&bar}", Link.REL_SELF) ;
		String uri = "http://localhost:8080/test/end?foo=%21";
		Link cleanLink = LinkUtils.cleanLink(link);
		Assert.assertEquals(uri, cleanLink.getHref());
	}
	
//	@Test
//	public void testCleanLinkPartial() {		
//		Link link = new Link("http://localhost:8080/test/end{?foo,bar}", Link.REL_SELF) ;
//		String uri = "http://localhost:8080/test/end{?foo}";
//		Link cleanLink = LinkUtils.removeTemplateVariable(link, "bar");
//		Assert.assertEquals(uri, cleanLink.getHref());
//	}
}
