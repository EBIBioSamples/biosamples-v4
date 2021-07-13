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
package uk.ac.ebi.biosamples.neo4j.repo;

import java.util.*;
import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.model.RelationshipType;
import uk.ac.ebi.biosamples.neo4j.NeoProperties;
import uk.ac.ebi.biosamples.neo4j.model.*;

@Component
public class NeoSampleRepository implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(NeoSampleRepository.class);

  private final Driver driver;

  public NeoSampleRepository(NeoProperties neoProperties) {
    driver =
        GraphDatabase.driver(
            neoProperties.getNeoUrl(),
            AuthTokens.basic(neoProperties.getNeoUsername(), neoProperties.getNeoPassword()));
  }

  @Override
  public void close() throws Exception {
    driver.close();
  }

  public List<Map<String, Object>> executeCypher(String cypherQuery) {
    List<Map<String, Object>> resultList;
    try (Session session = driver.session()) {
      Result result = session.run(cypherQuery);
      resultList = result.list(r -> r.asMap(NeoSampleRepository::convert));
    } catch (Exception e) {
      resultList = new ArrayList<>();
    }

    return resultList;
  }

  static Object convert(Value value) {
    switch (value.type().name()) {
      case "PATH":
        return value.asList(NeoSampleRepository::convert);
      case "NODE":
      case "RELATIONSHIP":
        return value.asMap();
    }
    return value.asObject();
  }

  public GraphSearchQuery graphSearch(GraphSearchQuery searchQuery, int limit, int page) {
    int skip = (page - 1) * limit;
    StringBuilder query = new StringBuilder();
    StringJoiner idJoiner = new StringJoiner(",");
    for (GraphNode node : searchQuery.getNodes()) {
      query.append("MATCH (").append(node.getId()).append(node.getQueryString()).append(") ");
      idJoiner.add(node.getId());
    }

    int relCount = 0;
    for (GraphLink link : searchQuery.getLinks()) {
      relCount++;
      String relName = "r" + relCount;
      query.append("MATCH ").append(link.getQueryString(relName));
      idJoiner.add(relName);
    }

    if (query.length() == 0) {
      query.append("MATCH(a1:Sample) ");
      idJoiner.add("a1");
    }

    StringBuilder countQuery = new StringBuilder(query.toString()).append(" RETURN COUNT(*)");
    query.append(" RETURN ").append(idJoiner.toString());
    query
        .append(" ORDER BY ")
        .append(idJoiner.toString().contains("a1") ? "a1" : "a2")
        .append(".accession SKIP ")
        .append(skip)
        .append(" LIMIT ")
        .append(limit);

    GraphSearchQuery response = new GraphSearchQuery();
    response.setPage(page);
    response.setSize(limit);
    try (Session session = driver.session()) {
      LOG.info("Graph query: {}", query);
      Result countResult = session.run(countQuery.toString());
      int totalElements = countResult.single().get(0).asInt();
      response.setTotalElements(totalElements);

      Result result = session.run(query.toString());
      Set<GraphNode> responseNodes = new HashSet<>();
      Set<GraphLink> responseLinks = new HashSet<>();
      response.setNodes(responseNodes);
      response.setLinks(responseLinks);

      while (result.hasNext()) {
        Record record = result.next();
        for (Value value : record.values()) {
          addToResponse(value, responseNodes, responseLinks);
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to load graph search results", e);
    }

    return response;
  }

  private void addToResponse(
      Value value, Set<GraphNode> responseNodes, Set<GraphLink> responseLinks) {
    switch (value.type().name()) {
      case "PATH":
        // todo handle PATH type
        LOG.warn("not handled yet");
        break;
      case "NODE":
        Node internalNode = value.asNode();
        GraphNode node = new GraphNode();
        node.setType(internalNode.labels().iterator().next());
        node.setAttributes((Map) internalNode.asMap());
        node.setId(String.valueOf(internalNode.id()));
        responseNodes.add(node);
        break;
      case "RELATIONSHIP":
        Relationship internalRel = value.asRelationship();
        GraphLink link = new GraphLink();
        link.setType(RelationshipType.getType(internalRel.type()));
        link.setStartNode(String.valueOf(internalRel.startNodeId()));
        link.setEndNode(String.valueOf(internalRel.endNodeId()));
        responseLinks.add(link);
        break;
      default:
        LOG.warn("Invalid neo4j value type: {}", value.type().name());
        break;
    }
  }

  /** ********************************************************************* */
  public void loadSample(NeoSample sample) {
    try (Session session = driver.session()) {
      createSample(session, sample);

      for (NeoRelationship relationship : sample.getRelationships()) {
        createSampleRelationship(session, relationship);
      }

      for (NeoExternalEntity ref : sample.getExternalRefs()) {
        createExternalRelationship(session, sample.getAccession(), ref);
      }
    }
  }

  public void createSample(Session session, NeoSample sample) {
    String query =
        "MERGE (a:Sample{accession:$accession}) " + "SET a.name = $name, a.taxid = $taxid";
    Map<String, String> sampleBasicInfoMap = new HashMap<>();
    sampleBasicInfoMap.put("accession", sample.getAccession());
    sampleBasicInfoMap.put("name", sample.getName());
    sampleBasicInfoMap.put("taxid", sample.getTaxId());

    Map<String, Object> params = new HashMap<>(sampleBasicInfoMap);

    if (sample.getOrganism() != null) {
      query = query + ", a.organism = $organism";
      params.put("organism", sample.getOrganism());
    }

    if (sample.getSex() != null) {
      query = query + ", a.sex = $sex";
      params.put("sex", sample.getSex());
    }

    if (sample.getCellType() != null) {
      query = query + ", a.celltype = $cellType";
      params.put("cellType", sample.getCellType());
    }

    if (sample.getMaterial() != null) {
      query = query + ", a.material = $material";
      params.put("material", sample.getMaterial());
    }

    if (sample.getProject() != null) {
      query = query + ", a.project = $project";
      params.put("project", sample.getProject());
    }

    if (sample.getCellLine() != null) {
      query = query + ", a.cellline = $cellLine";
      params.put("cellLine", sample.getCellLine());
    }

    if (sample.getOrganismPart() != null) {
      query = query + ", a.organismpart = $organismPart";
      params.put("organismPart", sample.getOrganismPart());
    }

    query = query + " RETURN a.accession";

    session.run(query, params);
  }

  public void createSampleRelationship(Session session, NeoRelationship relationship) {
    String query =
        "MERGE (a:Sample {accession:$fromAccession}) "
            + "MERGE (b:Sample {accession:$toAccession}) "
            + "MERGE (a)-[r:"
            + relationship.getType()
            + "]->(b)";
    Map<String, Object> params = new HashMap<>();
    params.put("fromAccession", relationship.getSource());
    params.put("toAccession", relationship.getTarget());

    session.run(query, params);
  }

  public void createExternalRelationship(
      Session session, String accession, NeoExternalEntity externalEntity) {
    String query =
        "MERGE (a:Sample {accession:$accession}) "
            + "MERGE (b:ExternalEntity {url:$url}) "
            + "SET b.archive = $archive, b.ref = $ref "
            + "MERGE (a)-[r:EXTERNAL_REFERENCE]->(b)";
    Map<String, Object> params = new HashMap<>();
    params.put("accession", accession);
    params.put("url", externalEntity.getUrl());
    params.put("archive", externalEntity.getArchive());
    params.put("ref", externalEntity.getRef());

    session.run(query, params);
  }

  public void createExternalEntity(
      Session session, String archive, String externalRef, String url) {
    String query =
        "MERGE (a:ExternalEntity{url:$url}) "
            + "SET a.archive = $archive, a.externalRef = $externalRef";
    Map<String, Object> params = new HashMap<>();
    params.put("url", url);
    params.put("archive", archive);
    params.put("externalRef", externalRef);

    session.run(query, params);
  }
}
