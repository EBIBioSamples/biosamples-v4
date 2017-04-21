package uk.ac.ebi.biosamples.neo.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Index;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity(label = "Curation")
public class NeoCuration {

	@GraphId
	private Long id;

    @Relationship(type = "HAS_PRE_ATTRIBUTE")
	private Set<NeoAttribute> attributesPre;
    
    @Relationship(type = "HAS_POST_ATTRIBUTE")    
	private Set<NeoAttribute> attributesPost;
    
    @Relationship(type = "APPLIED_TO")
    private Set<NeoCurationApplication> applications;
    
	@Property
	@Index(unique=true, primary=true)
	private String compositeIdentifier;

	private NeoCuration() {
	}
	
	public Long getId() {
		return id;
	}

	/**
	 * Do not modify this directly
	 * @return
	 */
	public Set<NeoAttribute> getAttributesPre() {
		return attributesPre;
	}


	/**
	 * Do not modify this directly
	 * @return
	 */
	public Set<NeoAttribute> getAttributesPost() {
		return attributesPost;
	}
	
	public Set<NeoCurationApplication> getApplications() {
		return applications;
	}
	
	public static NeoCuration build(Collection<NeoAttribute> attributesPre, Collection<NeoAttribute> attributesPost) {
		NeoCuration neoCuration = new NeoCuration();
		neoCuration.attributesPre = new TreeSet<>(attributesPre);
		neoCuration.attributesPost = new TreeSet<>(attributesPost);
		
		StringBuilder sb = new StringBuilder();
		Iterator<NeoAttribute> it = attributesPre.iterator();
		while (it.hasNext()) {
			NeoAttribute attribute = it.next();
			sb.append(attribute.getType());
			sb.append("|");
			sb.append(attribute.getValue());
			if (it.hasNext()) {
				sb.append("|");
			}
		}
		it = attributesPost.iterator();
		while (it.hasNext()) {
			NeoAttribute attribute = it.next();
			sb.append(attribute.getType());
			sb.append("|");
			sb.append(attribute.getValue());
			if (it.hasNext()) {
				sb.append("|");
			}
		}
		neoCuration.compositeIdentifier = sb.toString();
				
		return neoCuration;
	}
}
