package uk.ac.ebi.biosamples.neo.service;

import java.time.Instant;

import org.neo4j.ogm.typeconversion.AttributeConverter;


public class InstantConverter implements AttributeConverter<Instant, Long>
{
	@Override
	public Long toGraphProperty(Instant dateTime) {
		if (dateTime == null) return null;
	    return dateTime.toEpochMilli();
	}

	@Override
	public Instant toEntityAttribute(Long millis) {
		if (millis == null) return null;
        return Instant.ofEpochMilli(millis);
	}
}