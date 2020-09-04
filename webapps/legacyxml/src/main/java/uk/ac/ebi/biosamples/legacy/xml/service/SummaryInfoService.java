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
package uk.ac.ebi.biosamples.legacy.xml.service;

import java.math.BigInteger;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.legacyxml.SummaryInfo;

@Service
public class SummaryInfoService {

  public SummaryInfo fromPagedSampleResources(PagedResources<?> resources) {

    PageMetadata metadata = resources.getMetadata();

    SummaryInfo summaryInfo = new SummaryInfo();
    summaryInfo.setTotal(BigInteger.valueOf(metadata.getTotalElements()));
    summaryInfo.setFrom(BigInteger.valueOf((metadata.getNumber() * metadata.getSize()) + 1));
    summaryInfo.setTo(BigInteger.valueOf((metadata.getNumber() + 1) * metadata.getSize()));
    summaryInfo.setPageNumber(BigInteger.valueOf(metadata.getNumber() + 1));
    summaryInfo.setPageSize(BigInteger.valueOf(metadata.getSize()));

    return summaryInfo;
  }

  public uk.ac.ebi.biosamples.model.legacyxml.SummaryInfo fromPagedGroupResources(
      PagedResources<?> resources) {

    PageMetadata metadata = resources.getMetadata();

    SummaryInfo summaryInfo = new SummaryInfo();
    summaryInfo.setTotal(BigInteger.valueOf(metadata.getTotalElements()));
    summaryInfo.setFrom(BigInteger.valueOf((metadata.getNumber() * metadata.getSize()) + 1));
    summaryInfo.setTo(
        BigInteger.valueOf(
            Math.min(
                metadata.getTotalElements(), (metadata.getNumber() + 1) * metadata.getSize())));
    summaryInfo.setPageNumber(BigInteger.valueOf(metadata.getNumber() + 1));
    summaryInfo.setPageSize(BigInteger.valueOf(metadata.getSize()));

    return summaryInfo;
  }
}
