/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.model;

import com.redfin.sitemapgenerator.W3CDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/** @author mrelac */
@XmlAccessorType(value = XmlAccessType.NONE)
@XmlRootElement(name = "url")
public class XmlUrl {

  private final W3CDateFormat dateFormat = getDateFormat();

  private XmlUrl() {
    this(null, LocalDate.now(), ChangeFrequency.WEEKLY, Priority.MEDIUM);
  }

  //
  //    private XmlUrl(String loc, Priority priority) {
  //        lastmod = dateFormat.format(new Date());
  //        this.loc = loc;
  //        this.priority = priority.getValue();
  //        this.changefreq = ChangeFrequency.WEEKLY.getValue();
  //    }

  private XmlUrl(String loc, LocalDate lastModifiedDate, ChangeFrequency freq, Priority priority) {
    this.lastmod =
        dateFormat.format(Date.from(lastModifiedDate.atStartOfDay(ZoneId.of("GMT")).toInstant()));
    this.loc = loc;
    this.priority = priority.getValue();
    this.changefreq = freq.getValue();
  }

  private W3CDateFormat getDateFormat() {
    W3CDateFormat dateFormat = new W3CDateFormat(W3CDateFormat.Pattern.DAY);
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    return dateFormat;
  }

  @XmlElement private String loc;

  @XmlElement private String lastmod;

  @XmlElement private String changefreq;

  @XmlElement private String priority;

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

    Priority(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  public static class XmlUrlBuilder {
    private String loc;
    private LocalDate lastModification;
    private ChangeFrequency frequency;
    private Priority priority;

    public XmlUrlBuilder(String loc) {
      this.loc = loc;
      lastModification = LocalDate.now();
      frequency = ChangeFrequency.WEEKLY;
      priority = Priority.MEDIUM;
    }

    public XmlUrlBuilder lastModified(LocalDate date) {
      this.lastModification = date;
      return this;
    }

    public XmlUrlBuilder changeWithFrequency(ChangeFrequency freq) {
      this.frequency = freq;
      return this;
    }

    public XmlUrlBuilder hasPriority(Priority p) {
      this.priority = p;
      return this;
    }

    public XmlUrl build() {
      return new XmlUrl(this.loc, this.lastModification, this.frequency, this.priority);
    }
  }

  public enum ChangeFrequency {
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly"),
    YEARLY("yearly");

    private String freq;

    ChangeFrequency(String freq) {
      this.freq = freq;
    }

    public String getValue() {
      return this.freq;
    }
  }
}
