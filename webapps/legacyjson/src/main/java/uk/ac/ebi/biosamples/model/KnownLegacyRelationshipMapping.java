package uk.ac.ebi.biosamples.model;

import java.util.HashMap;
import java.util.Optional;

public final class KnownLegacyRelationshipMapping {

    private final HashMap<String, String> directToInverse;
    private final HashMap<String, String> inverseToDirect;

    public KnownLegacyRelationshipMapping() {
        this.directToInverse = new HashMap<>();
        this.inverseToDirect = new HashMap<>();

        this.add("derivedTo", "derivedFrom");
        this.add("parentOf", "childOf");
        this.add("recuratedTo", "recuratedFrom");
    }

    private void add(String inverse, String direct) {
        this.directToInverse.put(direct, inverse);
        this.inverseToDirect.put(inverse, direct);
    }

    public Optional<String> getMappendRelationship(String relationship) {

        String mappedRelationship = null;

        if (this.directToInverse.containsKey(relationship)) {
            mappedRelationship = this.directToInverse.get(relationship);
        } else if (this.inverseToDirect.containsKey(relationship)) {
            mappedRelationship = this.inverseToDirect.get(relationship);
        }

        return Optional.ofNullable(mappedRelationship);

    }

    public boolean isInverseRelationship(String relationship) {
        return this.inverseToDirect.containsKey(relationship);
    }

    public boolean isDirectRelationship(String relationship) { return this.directToInverse.containsKey(relationship); }
}
