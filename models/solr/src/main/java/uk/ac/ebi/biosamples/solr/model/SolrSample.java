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
package uk.ac.ebi.biosamples.solr.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.springframework.data.annotation.Id;
import org.springframework.data.solr.core.mapping.Dynamic;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;
import uk.ac.ebi.biosamples.solr.service.SolrFieldService;

@SolrDocument(solrCoreName = "samples")
public class SolrSample {

  /** Use the accession as the primary document identifier */
  @Id
  @Indexed(name = "id", required = true)
  protected String accession;

  @Indexed(
      name = "name_s",
      required = true,
      copyTo = {"autocomplete_ss"})
  protected String name;

  @Indexed(name = "domain_s", required = true)
  protected String domain;

  @Indexed(name = "webinId", required = true)
  protected String webinSubmissionAcccountId;

  // TODO
  /**
   * Store the release date as a string so that it can be used easily by solr Use a TrieDate type
   * for better range query performance
   */
  @Indexed(name = "release_dt", required = true, type = "date")
  protected String release;
  /**
   * Store the update date as a string so that it can be used easily by solr Use a TrieDate type for
   * better range query performance
   */
  @Indexed(name = "update_dt", required = true, type = "date") // TODO why type=date ?
  protected String update;

  @Indexed(name = "modified_dt", required = true, type = "date") // TODO why type=date ?
  protected String modified;

  @Indexed(name = "indexed_dt", required = true, type = "date") // TODO why type=date ?
  protected String indexed;

  @Indexed(name = "*_av_ss", copyTo = "autocomplete")
  @Dynamic
  protected Map<String, List<String>> attributeValues;

  @Indexed(
      name = "*_ai_ss",
      copyTo = {
        "ontologyiri_ss",
      })
  @Dynamic
  protected Map<String, List<String>> attributeIris;

  @Indexed(name = "*_au_ss")
  @Dynamic
  protected Map<String, List<String>> attributeUnits;

  /** Relationships for which this sample is the source */
  @Indexed(name = "*_or_ss")
  @Dynamic
  protected Map<String, List<String>> outgoingRelationships;

  /** Relationships for which this sample is the target */
  @Indexed(name = "*_ir_ss")
  @Dynamic
  protected Map<String, List<String>> incomingRelationships;

  /**
   * This field shouldn't be populated directly, instead Solr will copy all the ontology terms from
   * the attributes into it.
   */
  @Indexed(name = "ontologyiri_ss")
  protected List<String> ontologyIris;

  /** This field is used to store external references only */
  @Indexed(name = "*_erd_ss", copyTo = "facetfields_ss")
  @Dynamic
  protected Map<String, List<String>> externalReferencesData;

  /**
   * This field is required to get a list of attribute to use for faceting. * It includes attributes
   * and relationships of the sample Since faceting does not require it to be stored, it wont be to
   * save space.
   */
  @Indexed(
      name = "facetfields_ss",
      copyTo = {
        "autocomplete_ss",
      })
  protected List<String> facetFields;
  // TODO consider renaming as used only for faceting

  /**
   * This field is required to use with autocomplete faceting. Since faceting does not require it to
   * be stored, it wont be to save space
   */
  @Indexed(name = "autocomplete_ss")
  protected List<String> autocompleteTerms;

  /** This field is required to store the ontology expansion and attributes from related samples */
  @Indexed(name = "keywords_ss")
  protected List<String> keywords;

  public SolrSample() {}

  public String getAccession() {
    return accession;
  }

  public String getName() {
    return name;
  }

  public String getDomain() {
    return domain;
  }

  public String getWebinSubmissionAcccountId() {
    return webinSubmissionAcccountId;
  }

  public String getRelease() {
    return release;
  }

  public String getUpdate() {
    return update;
  }

  public String getModified() {
    return modified;
  }

  public String getIndexed() {
    return indexed;
  }

  public Map<String, List<String>> getAttributeValues() {
    return attributeValues;
  }

  public Map<String, List<String>> getAttributeIris() {
    return attributeIris;
  }

  public Map<String, List<String>> getAttributeUnits() {
    return attributeUnits;
  }

  public List<String> getOntologyIris() {
    return ontologyIris;
  }

  public Map<String, List<String>> getExternalReferencesData() {
    return externalReferencesData;
  }

  public Map<String, List<String>> getIncomingRelationships() {
    return incomingRelationships;
  }

  public Map<String, List<String>> getOutgoingRelationships() {
    return outgoingRelationships;
  }

  public List<String> getAutocompletes() {
    return autocompleteTerms;
  }

  public List<String> getKeywords() {
    return keywords;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Sample(");
    sb.append(name);
    sb.append(",");
    sb.append(accession);
    sb.append(",");
    sb.append(release);
    sb.append(",");
    sb.append(update);
    sb.append(",");
    sb.append(attributeValues);
    sb.append(",");
    sb.append(attributeIris);
    sb.append(",");
    sb.append(attributeUnits);
    sb.append(",");
    sb.append(outgoingRelationships);
    sb.append(",");
    sb.append(incomingRelationships);
    sb.append(",");
    sb.append(externalReferencesData);
    sb.append(")");
    return sb.toString();
  }

  /**
   * Avoid using this directly, use the SolrSampleToSampleConverter or SampleToSolrSampleConverter
   * instead
   */
  public static SolrSample build(
      String name,
      String accession,
      String domain,
      String webinSubmissionAcccountId,
      String release,
      String update,
      String modified,
      String indexed,
      Map<String, List<String>> attributeValues,
      Map<String, List<String>> attributeIris,
      Map<String, List<String>> attributeUnits,
      Map<String, List<String>> outgoingRelationships,
      Map<String, List<String>> incomingRelationships,
      Map<String, List<String>> externalReferencesData,
      List<String> keywords) {

    // TODO validate maps
    if (attributeValues == null) {
      attributeValues = new HashMap<>();
    }
    if (attributeIris == null) {
      attributeIris = new HashMap<>();
    }
    if (attributeUnits == null) {
      attributeUnits = new HashMap<>();
    }

    SolrSample sample = new SolrSample();
    sample.accession = accession;
    sample.name = name;
    sample.release = release;
    sample.update = update;
    sample.domain = domain;
    sample.webinSubmissionAcccountId = webinSubmissionAcccountId;
    sample.modified = modified;
    sample.indexed = indexed;
    sample.attributeValues = attributeValues;
    sample.attributeIris = attributeIris;
    sample.attributeUnits = attributeUnits;
    sample.incomingRelationships = incomingRelationships;
    sample.outgoingRelationships = outgoingRelationships;
    sample.externalReferencesData = externalReferencesData;

    SortedSet<String> facetFieldSet = new TreeSet<>();
    if (attributeValues != null && attributeValues.keySet().size() > 0) {
      for (String attributeValueKey : attributeValues.keySet()) {
        facetFieldSet.add(attributeValueKey + "_av_ss");
      }
    }

    if (outgoingRelationships != null && outgoingRelationships.keySet().size() > 0) {
      for (String outgoingRelationshipsKey : outgoingRelationships.keySet()) {
        facetFieldSet.add(outgoingRelationshipsKey + "_or_ss");
      }
    }

    if (incomingRelationships != null && incomingRelationships.keySet().size() > 0) {
      for (String incomingRelationshipsKey : incomingRelationships.keySet()) {
        facetFieldSet.add(incomingRelationshipsKey + "_ir_ss");
      }
    }

    if (externalReferencesData != null && externalReferencesData.keySet().size() > 0) {
      for (String externalReferencesDataKey : externalReferencesData.keySet()) {
        facetFieldSet.add(externalReferencesDataKey + "_erd_ss");
      }
    }

    sample.facetFields = new ArrayList<>(facetFieldSet);

    // copy into the other fields
    // this should be done in a copyfield but that doesn't work for some reason?
    sample.autocompleteTerms = new ArrayList<>();
    for (String key : attributeValues.keySet()) {
      sample.autocompleteTerms.add(SolrFieldService.decodeFieldName(key));
      sample.autocompleteTerms.addAll(attributeValues.get(key));
    }

    sample.keywords = new ArrayList<>();
    sample.keywords.addAll(keywords);

    return sample;
  }
}
