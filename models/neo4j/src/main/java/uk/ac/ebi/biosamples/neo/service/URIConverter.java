package uk.ac.ebi.biosamples.neo.service;

import java.net.URI;

import org.neo4j.ogm.typeconversion.AttributeConverter;

public class URIConverter implements AttributeConverter<URI, String> {

	@Override
	public URI toEntityAttribute(String string) {
		return URI.create(string);
	}

	@Override
	public String toGraphProperty(URI uri) {
		return uri.toString();
	}

}
