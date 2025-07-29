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
package uk.ac.ebi.biosamples.sitemap.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.bind.annotation.*;

/** @author mrelac */
@JacksonXmlRootElement(localName = "sitemapindex")
public class XmlSitemapIndex {
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "sitemap")
  private final Collection<XmlSitemap> xmlSitemaps = new ArrayList();

  public void addSitemap(final XmlSitemap xmlSitemap) {
    xmlSitemaps.add(xmlSitemap);
  }

  public Collection<XmlSitemap> getXmlSitemaps() {
    return xmlSitemaps;
  }
}
