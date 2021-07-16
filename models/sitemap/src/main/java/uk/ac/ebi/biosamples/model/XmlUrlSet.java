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
package uk.ac.ebi.biosamples.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

/** @author mrelac */
@XmlAccessorType(value = XmlAccessType.NONE)
@XmlRootElement(name = "urlset")
public class XmlUrlSet {

  private static final int MAX_SITEMAP_ENTRIES = 50000;

  @XmlElements({@XmlElement(name = "url", type = XmlUrl.class)})
  private final List<XmlUrl> xmlUrls = new ArrayList();

  public void addUrl(XmlUrl xmlUrl) {

    final int size = xmlUrls.size();

    // Reservoir sampling to get random entries in the model (this will get them all
    // eventually, but not exceed the model entry size restriction)
    if (size >= MAX_SITEMAP_ENTRIES) {
      // Randomly replace elements in the reservoir with a decreasing probability.
      int idx = new Double(Math.floor(Math.random() * size)).intValue();
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
