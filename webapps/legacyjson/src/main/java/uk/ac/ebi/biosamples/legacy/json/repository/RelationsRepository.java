package uk.ac.ebi.biosamples.legacy.json.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.legacy.json.domain.GroupsRelations;
import uk.ac.ebi.biosamples.legacy.json.domain.KnownRelationsMapping;
import uk.ac.ebi.biosamples.legacy.json.domain.SamplesRelations;
import uk.ac.ebi.biosamples.legacy.json.domain.SupportedGroupsRelationships;
import uk.ac.ebi.biosamples.legacy.json.domain.SupportedSamplesRelationships;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class RelationsRepository {

    private final KnownRelationsMapping relationshipMapping;
    private final SampleRepository sampleRepository;

    public RelationsRepository(SampleRepository sampleRepository) {
        this.relationshipMapping = new KnownRelationsMapping();
        this.sampleRepository = sampleRepository;
    }

    public List<GroupsRelations> getGroupsRelationships(String accession){
        Optional<Sample> sample = sampleRepository.findByAccession(accession);
        if (!sample.isPresent()) {
            return Collections.emptyList();
        }

        return groupsRelatedTo(sample.get()).stream()
                    .map(GroupsRelations::new)
                    .collect(Collectors.toList());
    }

    public List<SamplesRelations> getSamplesRelations(String accession, String relationshipType) {
        Optional<Sample> sample = sampleRepository.findByAccession(accession);
        if (!sample.isPresent()) {
            return Collections.emptyList();
        }

        return samplesRelatedTo(sample.get(), relationshipType).stream()
                .map(SamplesRelations::new)
                .collect(Collectors.toList());

    }

    private List<Sample> samplesRelatedTo(Sample sample, String relationType) {

        List<Relationship> validRelationships = new ArrayList<>();

        for (Relationship rel: sample.getRelationships()) {
            if (relationshipsOfTypeAndAccession(relationType).test(rel)) {
                validRelationships.add(rel);
            }
        }

        List<Sample> relatedSamples = new ArrayList<>();
        for (Relationship rel: validRelationships) {

            String relatedSampleAccession = rel.getSource().equals(sample.getAccession()) ? rel.getTarget() : rel.getSource();
            Optional<Sample> relSample = sampleRepository.findByAccession(relatedSampleAccession);
            relSample.ifPresent(relatedSamples::add);
        }
        return relatedSamples;
    }

    private List<Sample> groupsRelatedTo(Sample sample) {

        return sample.getRelationships().stream()
                .filter(groupRelationships())
                .map(r -> r.getType().equals("groups") ? r.getTarget() : r.getSource())
                .map(sampleRepository::findByAccession)
                .filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toList());

    }

    private Predicate<? super Relationship> groupRelationships() {
        return r -> r.getType().equals("group") || r.getType().equals("has member");
    }


    /**
     * Predicate to filter for relationships of a specific type, accounting also for inverse relationships
     * @param relationshipType
     * @return
     */
    private Predicate<Relationship> relationshipsOfTypeAndAccession(String relationshipType) {

        return r ->
                r.getType().equals(relationshipType) || this.relationshipUsesMappedType(r, relationshipType);

    }

    private boolean relationshipUsesMappedType(Relationship r, String otherType) {
        if (this.relationshipMapping.getMappedRelationship(otherType).isPresent())
            return r.getType().equals(this.relationshipMapping.getMappedRelationship(otherType).get());
        return false;
    }


    public boolean isSupportedSamplesRelation(String relationType) {
        return SupportedSamplesRelationships.getFromName(relationType) != null;
    }

    public boolean isSupportedGroupsRelations(String relationType) {
        return SupportedGroupsRelationships.getFromName(relationType) != null;
    }
}
