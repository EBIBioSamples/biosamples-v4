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
package uk.ac.ebi.biosamples.utils;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class LinkUtilsTest {
  @Test
  public void testCleanLink() {
    Link link = Link.of("http://localhost:8080/test/end{?foo,bar}", IanaLinkRelations.SELF);
    String uri = "http://localhost:8080/test/end";
    Link cleanLink = LinkUtils.cleanLink(link);
    Assert.assertEquals(uri, cleanLink.getHref());
  }

  @Test
  @Ignore
  public void testCleanLinkEncoded() {
    Link link = Link.of("http://localhost:8080/test/end?foo=%21{&bar}", IanaLinkRelations.SELF);
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
