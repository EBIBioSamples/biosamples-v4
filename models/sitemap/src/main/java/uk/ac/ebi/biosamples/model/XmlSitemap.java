/*******************************************************************************
 * Copyright 2015 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 *******************************************************************************/

package uk.ac.ebi.biosamples.model;

import com.redfin.sitemapgenerator.W3CDateFormat;

import javax.xml.bind.annotation.XmlElement;
import java.util.Date;
import java.util.TimeZone;

/**
 * Class representing the sitemap entry
 */
public class XmlSitemap {
    
    private W3CDateFormat dateFormat;
    
    public XmlSitemap() {
        this("");
    }
    public XmlSitemap(String loc) {
        dateFormat = new W3CDateFormat(W3CDateFormat.Pattern.DAY);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        lastmod = dateFormat.format(new Date());
        this.loc = loc;
    }

    @XmlElement
    private final String loc;

    @XmlElement
    private final String lastmod;
    
    public String getLoc() {
        return loc;
    }

    public String getLastmod() {
        return lastmod;
    }
}
