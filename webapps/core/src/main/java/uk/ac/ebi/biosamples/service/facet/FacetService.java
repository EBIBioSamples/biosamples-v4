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
package uk.ac.ebi.biosamples.service.facet;

import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import uk.ac.ebi.biosamples.core.model.facet.Facet;
import uk.ac.ebi.biosamples.core.model.filter.Filter;

public interface FacetService {
  List<Facet> getFacets(
      String searchTerm,
      Set<Filter> filters,
      String webinId,
      Pageable facetFieldPageInfo,
      Pageable facetValuesPageInfo,
      String facetField,
      List<String> facetFields);
}
