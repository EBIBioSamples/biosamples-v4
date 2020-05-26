package uk.ac.ebi.biosamples.neoexport;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.neo4j.model.NeoExternalEntity;
import uk.ac.ebi.biosamples.neo4j.model.NeoRelationship;
import uk.ac.ebi.biosamples.neo4j.model.NeoSample;

import java.io.*;
import java.util.*;

@Component
public class NeoCsvExporter {
    private static final Logger LOG = LoggerFactory.getLogger(NeoCsvExporter.class);

    private static final String EXPORT_PATH = "./export/";
    private static final String REL_SOURCE_HEADER = ":START_ID(Sample)";
    private static final String REL_TARGET_HEADER = ":END_ID(Sample)";
    private static final int PERSIST_THRESHOLD = 1000000;

    private List<Map<String, String>> samples = new ArrayList<>();
    private List<Map<String, String>> externalEntity = new ArrayList<>();

    private List<Map<String, String>> relsDerivedFrom = new ArrayList<>();
    private List<Map<String, String>> relsSameAs = new ArrayList<>();
    private List<Map<String, String>> relsHasMember = new ArrayList<>();
    private List<Map<String, String>> relsChildOf = new ArrayList<>();
    private List<Map<String, String>> relsOther = new ArrayList<>();
    private List<Map<String, String>> relsExternalRef = new ArrayList<>();

    private int sampleIndex = 1;
    private int externalEntityIndex = 1;
    private int relsExternelRefIndex = 1;
    private int mockIndex = 1;


    public void addToCSVFile(Sample sample) {
        NeoSample neoSample = NeoSample.build(sample);
        addSample(neoSample);
    }

    public void flush() {
        writeCSV(samples, "samples-" + sampleIndex + ".csv", false);
        writeCSV(externalEntity, "ex_reference-" + externalEntityIndex + ".csv", false);

        writeCSV(relsExternalRef, "external_reference-" + relsExternelRefIndex + ".csv", false);
        writeCSV(relsDerivedFrom, "derived_from-" + mockIndex + ".csv", true);
        writeCSV(relsSameAs, "same_as-" + mockIndex + ".csv", true);
        writeCSV(relsHasMember, "has_member-" + mockIndex + ".csv", true);
        writeCSV(relsChildOf, "child_of-" + mockIndex + ".csv", true);
        writeCSV(relsOther, "other-" + mockIndex + ".csv", true);
    }

    private void addSample(NeoSample sample) {
        Map<String, String> sampleMap = Map.of(
                "accession:ID(Sample)", sample.getAccession(),
                "name", sample.getName(),
                "organism", sample.getOrganism() == null ? "" : sample.getOrganism(),
                "taxId", sample.getTaxId() == null ? "" : sample.getTaxId(),
                "sex", sample.getSex() == null ? "" : sample.getSex(),
                "cellType", sample.getCellType() == null ? "" : sample.getCellType(),
                "material", sample.getMaterial() == null ? "" : sample.getMaterial(),
                "project", sample.getProject() == null ? "" : sample.getProject(),
                "cellLine", sample.getCellLine() == null ? "" : sample.getCellLine(),
                "organismPart", sample.getOrganismPart() == null ? "" : sample.getOrganismPart());
        samples.add(sampleMap);

        for (NeoRelationship rel : sample.getRelationships()) {
            if (rel.getSource().equals(sample.getAccession())) {
                switch (rel.getType()) {
                    case DERIVED_FROM:
                        relsDerivedFrom.add(Map.of(REL_SOURCE_HEADER, rel.getSource(), REL_TARGET_HEADER, rel.getTarget()));
                        break;
                    case SAME_AS:
                        relsSameAs.add(Map.of(REL_SOURCE_HEADER, rel.getSource(), REL_TARGET_HEADER, rel.getTarget()));
                        break;
                    case HAS_MEMBER:
                        relsHasMember.add(Map.of(REL_SOURCE_HEADER, rel.getSource(), REL_TARGET_HEADER, rel.getTarget()));
                        break;
                    case CHILD_OF:
                        relsChildOf.add(Map.of(REL_SOURCE_HEADER, rel.getSource(), REL_TARGET_HEADER, rel.getTarget()));
                        break;
                    default:
                        relsOther.add(Map.of(REL_SOURCE_HEADER, rel.getSource(), REL_TARGET_HEADER, rel.getTarget()));
                        break;
                }
            }
        }

        for (NeoExternalEntity ref : sample.getExternalRefs()) {
            String refId = ref.getArchive() + "_" + ref.getRef();
            externalEntity.add(Map.of("name:ID(ExternalEntity)", refId, "archive", ref.getArchive(), "ref", ref.getRef(), "url", ref.getUrl()));
            relsExternalRef.add(Map.of(REL_SOURCE_HEADER, sample.getAccession(), ":END_ID(ExternalEntity)", refId));
        }

        checkWriteStatus();
    }

    private void checkWriteStatus() {
        if (samples.size() >= PERSIST_THRESHOLD) {
            writeCSV(samples, "samples-" + sampleIndex + ".csv", false);
            samples.clear();
            sampleIndex++;
        }
        if (externalEntity.size() >= PERSIST_THRESHOLD) {
            writeCSV(externalEntity, "ex_reference-" + externalEntityIndex + ".csv", false);
            externalEntity.clear();
            externalEntityIndex++;
        }
        if (relsExternalRef.size() >= PERSIST_THRESHOLD) {
            writeCSV(relsExternalRef, "external_reference-" + relsExternelRefIndex + ".csv", false);
            relsExternalRef.clear();
            relsExternelRefIndex++;
        }


        if (relsDerivedFrom.size() >= PERSIST_THRESHOLD) {
            writeCSV(relsDerivedFrom, "derived_from-" + mockIndex + ".csv", true);
            relsDerivedFrom.clear();
        }
        if (relsSameAs.size() >= PERSIST_THRESHOLD) {
            writeCSV(relsSameAs, "same_as-" + mockIndex + ".csv", true);
            relsSameAs.clear();
        }
        if (relsHasMember.size() >= PERSIST_THRESHOLD) {
            writeCSV(relsHasMember, "has_member-" + mockIndex + ".csv", true);
            relsHasMember.clear();
        }
        if (relsChildOf.size() >= PERSIST_THRESHOLD) {
            writeCSV(relsChildOf, "child_of-" + mockIndex + ".csv", true);
            relsChildOf.clear();
        }
        if (relsOther.size() >= PERSIST_THRESHOLD) {
            writeCSV(relsOther, "other-" + mockIndex + ".csv", true);
            relsOther.clear();
        }
    }


    private void writeCSV(List<Map<String, String>> records, String fileName, boolean append) {
        CsvSchema schema = null;
        CsvSchema.Builder schemaBuilder = CsvSchema.builder();
        if (records != null && !records.isEmpty()) {
            for (String col : records.get(0).keySet()) {
                schemaBuilder.addColumn(col);
            }
            schema = schemaBuilder.build();
        }

        CsvMapper mapper = new CsvMapper();
        File file = new File(EXPORT_PATH + fileName);
        try (Writer writer = new FileWriter(file, append)){
            mapper.writer(schema).writeValues(writer).writeAll(records);
            writer.flush();
        } catch (IOException e) {
            LOG.error("Failed writing to csv file: {}", fileName, e);
        }
    }
}
