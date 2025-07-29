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
import uk.ac.ebi.biosamples.core.model.RelationshipType;
import uk.ac.ebi.biosamples.neo4j.NeoProperties;
import uk.ac.ebi.biosamples.neo4j.model.*;

@Component
public class NeoSampleRepository implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(NeoSampleRepository.class);

  private final Driver driver;

  public NeoSampleRepository(final NeoProperties neoProperties) {
    driver =
        GraphDatabase.driver(
            neoProperties.getNeoUrl(),
            AuthTokens.basic(neoProperties.getNeoUsername(), neoProperties.getNeoPassword()));
  }

  @Override
  public void close() throws Exception {
    driver.close();
  }

  public List<Map<String, Object>> executeCypher(final String cypherQuery) {
    List<Map<String, Object>> resultList;
    try (final Session session = driver.session()) {
      final Result result = session.run(cypherQuery);
      resultList = result.list(r -> r.asMap(NeoSampleRepository::convert));
    } catch (final Exception e) {
      resultList = new ArrayList<>();
    }

    return resultList;
  }

  private static Object convert(final Value value) {
    switch (value.type().name()) {
      case "PATH":
        return value.asList(NeoSampleRepository::convert);
      case "NODE":
      case "RELATIONSHIP":
        return value.asMap();
    }
    return value.asObject();
  }

  public GraphSearchQuery graphSearch(
      final GraphSearchQuery searchQuery, final int limit, final int page) {
    final int skip = (page - 1) * limit;
    final StringBuilder query = new StringBuilder();
    final StringJoiner idJoiner = new StringJoiner(",");
    for (final GraphNode node : searchQuery.getNodes()) {
      query.append("MATCH (").append(node.getId()).append(node.getQueryString()).append(") ");
      idJoiner.add(node.getId());
    }

    int relCount = 0;
    for (final GraphLink link : searchQuery.getLinks()) {
      relCount++;
      final String relName = "r" + relCount;
      query.append("MATCH ").append(link.getQueryString(relName));
      idJoiner.add(relName);
    }

    if (query.length() == 0) {
      query.append("MATCH(a1:Sample) ");
      idJoiner.add("a1");
    }

    final StringBuilder countQuery = new StringBuilder(query.toString()).append(" RETURN COUNT(*)");
    query.append(" RETURN ").append(idJoiner.toString());
    query
        .append(" ORDER BY ")
        .append(idJoiner.toString().contains("a1") ? "a1" : "a2")
        .append(".accession SKIP ")
        .append(skip)
        .append(" LIMIT ")
        .append(limit);

    final GraphSearchQuery response = new GraphSearchQuery();
    response.setPage(page);
    response.setSize(limit);
    try (final Session session = driver.session()) {
      LOG.info("Graph query: {}", query);
      final Result countResult = session.run(countQuery.toString());
      final int totalElements = countResult.single().get(0).asInt();
      response.setTotalElements(totalElements);

      final Result result = session.run(query.toString());
      final Set<GraphNode> responseNodes = new HashSet<>();
      final Set<GraphLink> responseLinks = new HashSet<>();
      response.setNodes(responseNodes);
      response.setLinks(responseLinks);

      while (result.hasNext()) {
        final org.neo4j.driver.Record record = result.next();
        for (final Value value : record.values()) {
          addToResponse(value, responseNodes, responseLinks);
        }
      }
    } catch (final Exception e) {
      LOG.error("Failed to load graph search results", e);
    }

    return response;
  }

  private void addToResponse(
      final Value value, final Set<GraphNode> responseNodes, final Set<GraphLink> responseLinks) {
    switch (value.type().name()) {
      case "PATH":
        // todo handle PATH type
        LOG.warn("not handled yet");
        break;
      case "NODE":
        final Node internalNode = value.asNode();
        final GraphNode node = new GraphNode();
        node.setType(internalNode.labels().iterator().next());
        node.setAttributes((Map) internalNode.asMap());
        node.setId(String.valueOf(internalNode.id()));
        responseNodes.add(node);
        break;
      case "RELATIONSHIP":
        final Relationship internalRel = value.asRelationship();
        final GraphLink link = new GraphLink();
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
  public void loadSample(final NeoSample sample) {
    try (final Session session = driver.session()) {
      createSample(session, sample);

      for (final NeoRelationship relationship : sample.getRelationships()) {
        createSampleRelationship(session, relationship);
      }

      for (final NeoExternalEntity ref : sample.getExternalRefs()) {
        createExternalRelationship(session, sample.getAccession(), ref);
      }
    }
  }

  private void createSample(final Session session, final NeoSample sample) {
    String query =
        "MERGE (a:Sample{accession:$accession}) " + "SET a.name = $name, a.taxid = $taxid";
    final Map<String, String> sampleBasicInfoMap = new HashMap<>();
    sampleBasicInfoMap.put("accession", sample.getAccession());
    sampleBasicInfoMap.put("name", sample.getName());
    sampleBasicInfoMap.put("taxid", sample.getTaxId());

    final Map<String, Object> params = new HashMap<>(sampleBasicInfoMap);

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

  private void createSampleRelationship(final Session session, final NeoRelationship relationship) {
    final String query =
        "MERGE (a:Sample {accession:$fromAccession}) "
            + "MERGE (b:Sample {accession:$toAccession}) "
            + "MERGE (a)-[r:"
            + relationship.getType()
            + "]->(b)";
    final Map<String, Object> params = new HashMap<>();
    params.put("fromAccession", relationship.getSource());
    params.put("toAccession", relationship.getTarget());

    session.run(query, params);
  }

  private void createExternalRelationship(
      final Session session, final String accession, final NeoExternalEntity externalEntity) {
    final String query =
        "MERGE (a:Sample {accession:$accession}) "
            + "MERGE (b:ExternalEntity {url:$url}) "
            + "SET b.archive = $archive, b.ref = $ref "
            + "MERGE (a)-[r:EXTERNAL_REFERENCE]->(b)";
    final Map<String, Object> params = new HashMap<>();
    params.put("accession", accession);
    params.put("url", externalEntity.getUrl());
    params.put("archive", externalEntity.getArchive());
    params.put("ref", externalEntity.getRef());

    session.run(query, params);
  }

  public void createExternalEntity(
      final Session session, final String archive, final String externalRef, final String url) {
    final String query =
        "MERGE (a:ExternalEntity{url:$url}) "
            + "SET a.archive = $archive, a.externalRef = $externalRef";
    final Map<String, Object> params = new HashMap<>();
    params.put("url", url);
    params.put("archive", archive);
    params.put("externalRef", externalRef);

    session.run(query, params);
  }
}
