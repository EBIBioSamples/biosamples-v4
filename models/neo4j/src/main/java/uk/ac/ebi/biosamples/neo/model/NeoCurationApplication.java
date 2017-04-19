package uk.ac.ebi.biosamples.neo.model;

import java.time.LocalDateTime;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import uk.ac.ebi.biosamples.neo.service.LocalDateTimeConverter;

@RelationshipEntity(type = "APPLIED_TO")
public class NeoCurationApplication  {

	@GraphId
	private Long id;
	
	@StartNode
	private NeoCuration curation;
	@EndNode
	private NeoSample target;

	@Convert(LocalDateTimeConverter.class)
	@Property
	private LocalDateTime time;

	private NeoCurationApplication() {
	};

	public Long getId() {
		return id;
	}

	public NeoCuration getCuration() {
		return curation;
	}

	public NeoSample getTarget() {
		return target;
	}

	public LocalDateTime getTime() {
		return time;
	}

	public static NeoCurationApplication build(NeoCuration curation, NeoSample target, LocalDateTime time) {
		NeoCurationApplication newRelationship = new NeoCurationApplication();
		newRelationship.curation = curation;
		newRelationship.target = target;
		newRelationship.time = time;
		return newRelationship;
	}
}
