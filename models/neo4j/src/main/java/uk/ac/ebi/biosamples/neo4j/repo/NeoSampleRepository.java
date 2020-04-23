package uk.ac.ebi.biosamples.neo4j.repo;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.neo4j.NeoProperties;
import uk.ac.ebi.biosamples.neo4j.model.NeoExternalEntity;
import uk.ac.ebi.biosamples.neo4j.model.NeoRelationship;
import uk.ac.ebi.biosamples.neo4j.model.NeoSample;

import java.util.Map;

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
        String query = "MERGE (a:Sample{accession:$accession}) " +
                "SET a.name = $name, a.taxid = $taxid " +
                "RETURN a.accession";
        Map<String, Object> params = Map.of(
                "accession", sample.getAccession(),
                "name", sample.getName(),
                "taxid", sample.getTaxId());

//        params.put("organims", sample.getOrganism());
//        params.put("sex", sample.getSex());

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

    public void createExternalEntity(Session session, String archive, String externalRef, String url) {
        String query = "MERGE (a:ExternalEntity{url:$url}) " +
                "SET a.archive = $archive, a.externalRef = $externalRef";
        Map<String, Object> params = Map.of(
                "url", url,
                "archive", archive,
                "externalRef", externalRef);

        session.run(query, params);
    }
}