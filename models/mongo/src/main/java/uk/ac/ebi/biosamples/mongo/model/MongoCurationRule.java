package uk.ac.ebi.biosamples.mongo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Objects;

@Document
public class MongoCurationRule implements Comparable<MongoCurationRule> {

    @Id
    private String id;
    private String attributePre;
    private String attributePost;
    private final Instant created;

    private MongoCurationRule(String attributePre, String attributePost) {
        this.attributePre = attributePre;
        this.attributePost = attributePost;
        id = attributePre;
        created = Instant.now();
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("attributePre")
    public String getAttributePre() {
        return attributePre;
    }

    @JsonProperty("attributePost")
    public String getAttributePost() {
        return attributePost;
    }

    @JsonProperty("created")
    public Instant getCreated() {
        return created;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributePre);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof MongoCurationRule)) {
            return false;
        }
        MongoCurationRule other = (MongoCurationRule) o;
        return Objects.equals(this.attributePre, other.attributePre)
                && Objects.equals(this.attributePost, other.attributePost)
                && Objects.equals(this.created, other.created);
    }

    @Override
    public int compareTo(MongoCurationRule other) {
        if (other == null) {
            return 1;
        }

        if (!this.attributePre.equals(other.attributePre)) {
            return this.attributePre.compareTo(other.attributePre);
        } else if (!this.attributePost.equals(other.attributePost)) {
            return this.attributePost.compareTo(other.attributePost);
        } else if (!this.created.equals(other.created)) {
            return this.created.compareTo(other.created);
        }

        return 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MongoCurationRule(");
        sb.append(attributePre);
        sb.append(",");
        sb.append(attributePost);
        sb.append(",");
        sb.append(created);
        sb.append(")");
        return sb.toString();
    }

    @JsonCreator
    public static MongoCurationRule build(@JsonProperty("attributePre") String attributePre,
                                          @JsonProperty("attributePost") String attributePost) {
        return new MongoCurationRule(attributePre, attributePost);
    }
}

