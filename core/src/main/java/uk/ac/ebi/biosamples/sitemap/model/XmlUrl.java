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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.redfin.sitemapgenerator.W3CDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

@JacksonXmlRootElement(localName = "url")
public class XmlUrl {

  private final W3CDateFormat dateFormat = getDateFormat();

  private XmlUrl() {
    this(null, LocalDate.now(), ChangeFrequency.WEEKLY, Priority.MEDIUM);
  }

  private XmlUrl(
      final String loc,
      final LocalDate lastModifiedDate,
      final ChangeFrequency freq,
      final Priority priority) {
    lastmod =
        dateFormat.format(Date.from(lastModifiedDate.atStartOfDay(ZoneId.of("GMT")).toInstant()));
    this.loc = loc;
    this.priority = priority.getValue();
    changefreq = freq.getValue();
  }

  private W3CDateFormat getDateFormat() {
    final W3CDateFormat dateFormat = new W3CDateFormat(W3CDateFormat.Pattern.DAY);
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    return dateFormat;
  }

  @JacksonXmlProperty(localName = "loc")
  private final String loc;

  @JacksonXmlProperty(localName = "lastmod")
  private final String lastmod;

  @JacksonXmlProperty(localName = "changefreq")
  private final String changefreq;

  @JacksonXmlProperty(localName = "priority")
  private final String priority;

  public String getLoc() {
    return loc;
  }

  public String getPriority() {
    return priority;
  }

  public String getChangefreq() {
    return changefreq;
  }

  public String getLastmod() {
    return lastmod;
  }

  public enum Priority {
    HIGH("1.0"),
    MEDIUM("0.5");

    private final String value;

    Priority(final String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  public static class XmlUrlBuilder {
    private final String loc;
    private LocalDate lastModification;
    private ChangeFrequency frequency;
    private Priority priority;

    public XmlUrlBuilder(final String loc) {
      this.loc = loc;
      lastModification = LocalDate.now();
      frequency = ChangeFrequency.WEEKLY;
      priority = Priority.MEDIUM;
    }

    public XmlUrlBuilder lastModified(final LocalDate date) {
      lastModification = date;
      return this;
    }

    public XmlUrlBuilder changeWithFrequency(final ChangeFrequency freq) {
      frequency = freq;
      return this;
    }

    public XmlUrlBuilder hasPriority(final Priority p) {
      priority = p;
      return this;
    }

    public XmlUrl build() {
      return new XmlUrl(loc, lastModification, frequency, priority);
    }
  }

  public enum ChangeFrequency {
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly"),
    YEARLY("yearly");

    private final String freq;

    ChangeFrequency(final String freq) {
      this.freq = freq;
    }

    public String getValue() {
      return freq;
    }
  }
}
