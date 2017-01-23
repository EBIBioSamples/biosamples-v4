package uk.ac.ebi.biosamples.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.dom4j.Document;
import org.dom4j.Element;

/**
 * A fluent-style builder for interrogating XML documents and elements
 * 
 * @author faulcon
 *
 */
public class XmlPathBuilder {
	
	private List<String> pathParts = new ArrayList<>();
	private Element root = null;
	
	protected XmlPathBuilder(Element root) {
		this.root = root;
	}
	
	public XmlPathBuilder path(String... pathParts) {
		this.pathParts.addAll(Arrays.asList(pathParts));		
		return this;
	}
	
	public Element element() {
		Element target = root;
		for (String pathPart : pathParts) {
			target = target.element(pathPart);
		}
		return target;
	}
	
	@SuppressWarnings("unchecked")
	public List<Element> elements() {
		Element target = root;
		for (String pathPart : pathParts) {
			target = target.element(pathPart);
		}
		//API pre-dates generics
		return (List<Element>) target.elements();
	}
	
	public List<Element> elements(String name) {
		return elements().stream().filter(e -> e.getName().equals(name)).collect(Collectors.toList());
	}
	
	public String text() {
		Element target = root;
		for (String pathPart : pathParts) {
			target = target.element(pathPart);
			if (target == null) {
				throw new IllegalArgumentException("Path path "+pathPart+" does not exist");
			}
		}
		return target.getTextTrim();
	}
	
	public boolean exists() {
		Element target = root;
		for (String pathPart : pathParts) {
			target = target.element(pathPart);
			if (target == null) {
				return false;
			}
		}
		return true;
	}
	
	public String attribute(String name) {
		Element target = root;
		for (String pathPart : pathParts) {
			target = target.element(pathPart);
			if (target == null) {
				throw new IllegalArgumentException("Path path "+pathPart+" does not exist");
			}
		}
		if (target.attribute(name) == null) {
			throw new IllegalArgumentException("Argument "+name+" does not exist at path "+String.join("/",pathParts));
		}
		return target.attributeValue(name);
	}
	
	public boolean attributeExists(String name) {
		Element target = root;
		for (String pathPart : pathParts) {
			target = target.element(pathPart);
			if (target == null) {
				return false;
			}
		}
		if (target.attribute(name) == null) {
			return false;
		}
		return true;
	}

	static public XmlPathBuilder of(Document doc){
		return new XmlPathBuilder(doc.getRootElement());
	}
	
	static public XmlPathBuilder of(Element root){
		return new XmlPathBuilder(root);
	}
}
