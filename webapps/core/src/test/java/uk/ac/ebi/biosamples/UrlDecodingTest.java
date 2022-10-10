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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.UriTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.util.UriUtils;

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
    Assert.assertEquals(
        "AoErVGVzdEZpbHRlcjM=", URLDecoder.decode("AoErVGVzdEZpbHRlcjM%3D", "UTF-8"));
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
    String uriTemplated =
        "http://localhost:8081/biosamples/samples?text=&cursor=AoErVGVzdEZpbHRlcjM%3D&size=1000{&filter,page,sort}";
    String uriUntemplated =
        "http://localhost:8081/biosamples/samples?text=&cursor=AoErVGVzdEZpbHRlcjM%3D&size=1000";
    Assert.assertEquals(
        uriUntemplated,
        UriTemplate.of(uriTemplated.replaceAll("\\{.*\\}", "")).expand().toString());
  }
}
