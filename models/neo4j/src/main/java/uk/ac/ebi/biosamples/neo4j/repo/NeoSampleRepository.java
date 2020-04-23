package uk.ac.ebi.biosamples.neo4j.repo;

import org.neo4j.driver.*;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo4j.NeoProperties;
import uk.ac.ebi.biosamples.neo4j.model.NeoExternalEntity;
import uk.ac.ebi.biosamples.neo4j.model.NeoRelationship;
import uk.ac.ebi.biosamples.neo4j.model.NeoSample;

import java.util.HashMap;
import java.util.Map;

import static org.neo4j.driver.Values.parameters;

@Component
public class NeoSampleRepository implements AutoCloseable {
    private final Driver driver;

    public NeoSampleRepository(NeoProperties neoProperties) {
        driver = GraphDatabase.driver(neoProperties.getNeoUrl(),
                AuthTokens.basic(neoProperties.getNeoUsername(), neoProperties.getNeoPassword()));
    }

    @Override
    public void close() throws Exception {
        driver.close();
    }

    public void createSample(NeoSample sample) {
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
        String query = "MERGE (a:Sample{accession:$accession}) " +
                "SET a.name = $name, a.taxid = $taxid " +
                "RETURN a.accession";
        Map<String, Object> params = Map.of(
                "accession", sample.getAccession(),
                "name", sample.getName(),
                "taxid", sample.getTaxId());

        session.run(query, params);
    }

    public void createSampleRelationship(Session session, NeoRelationship relationship) {
        String query = "MERGE (a:Sample {accession:$fromAccession}) " +
                "MERGE (b:Sample {accession:$toAccession}) " +
                "MERGE (a)-[r:" + relationship.getType() + "]->(b)";
        Map<String, Object> params = Map.of(
                "fromAccession", relationship.getSource(),
                "toAccession", relationship.getTarget());

        session.run(query, params);
    }

    public void createExternalRelationship(Session session, String accession, NeoExternalEntity externalEntity) {
        String query = "MERGE (a:Sample {accession:$accession}) " +
                "MERGE (b:ExternalEntity {url:$url, archive:$archive, ref:$ref}) " +
                "MERGE (a)-[r:EXTERNAL_REFERENCE]->(b)";
        Map<String, Object> params = Map.of(
                "accession", accession,
                "url", externalEntity.getUrl(),
                "archive", externalEntity.getArchive(),
                "ref", externalEntity.getRef());

        session.run(query, params);
    }

    public void createExternalEntity(String archive, String externalRef, String url) {
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                Result result = tx.run("MERGE (a:ExternalEntity{url:$url}) " +
                                "SET a.archive = $archive, a.externalRef = $externalRef",
                        parameters("url", url,
                                "archive", archive, "externalRef", externalRef));
                return result.single().get(0).asString();
            });
        }
    }
}