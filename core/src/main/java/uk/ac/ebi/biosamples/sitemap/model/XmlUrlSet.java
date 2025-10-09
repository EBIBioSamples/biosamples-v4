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
import java.util.List;

@JacksonXmlRootElement(localName = "urlset")
public class XmlUrlSet {

  private static final int MAX_SITEMAP_ENTRIES = 50000;

  @JacksonXmlProperty(localName = "url")
  @JacksonXmlElementWrapper(useWrapping = false)
  private final List<XmlUrl> xmlUrls = new ArrayList<>();

  public void addUrl(final XmlUrl xmlUrl) {
    final int size = xmlUrls.size();

    // Reservoir sampling
    if (size >= MAX_SITEMAP_ENTRIES) {
      final int idx = (int) Math.floor(Math.random() * size);
      if (idx < MAX_SITEMAP_ENTRIES) {
        xmlUrls.set(idx, xmlUrl);
      }
    } else {
      xmlUrls.add(xmlUrl);
    }
  }

  public Collection<XmlUrl> getXmlUrls() {
    return xmlUrls;
  }
}
