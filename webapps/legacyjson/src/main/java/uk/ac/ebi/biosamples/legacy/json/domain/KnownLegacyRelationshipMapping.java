package uk.ac.ebi.biosamples.legacy.json.domain;

import java.util.HashMap;
import java.util.Optional;

import static uk.ac.ebi.biosamples.legacy.json.domain.SupportedSamplesRelationships.*;

public final class KnownLegacyRelationshipMapping {

    private final HashMap<SupportedSamplesRelationships, SupportedSamplesRelationships> directToInverse;
    private final HashMap<SupportedSamplesRelationships, SupportedSamplesRelationships> inverseToDirect;

    public KnownLegacyRelationshipMapping() {
        this.directToInverse = new HashMap<>();
        this.inverseToDirect = new HashMap<>();

        this.add(DERIVE_TO, DERIVE_FROM);
        this.add(PARENT_OF, CHILD_OF);
        this.add(RECURATED_TO, RECURATED_FROM);
    }

    private void add(SupportedSamplesRelationships inverse, SupportedSamplesRelationships direct) {
        this.directToInverse.put(direct, inverse);
        this.inverseToDirect.put(inverse, direct);
    }

    public Optional<String> getMappedRelationship(String relationship) {

        SupportedSamplesRelationships rel = getFromName(relationship);
        String mappedRelationship = null;

        if (rel != null) {
            if (this.directToInverse.containsKey(rel)) {
                mappedRelationship = this.directToInverse.get(rel).getRelationshipName();
            } else if (this.inverseToDirect.containsKey(rel)) {
                mappedRelationship = this.inverseToDirect.get(rel).getRelationshipName();
            }
        }

        return Optional.ofNullable(mappedRelationship);

    }

}
