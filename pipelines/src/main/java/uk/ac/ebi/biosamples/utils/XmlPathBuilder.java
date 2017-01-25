package uk.ac.ebi.biosamples.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
			if (target == null) {
				throw new IllegalArgumentException("Path path "+pathPart+" does not exist");
			}
		}
		return target;
	}
	
	@SuppressWarnings("unchecked")
	public List<Element> elements() {
		
		List<Element> elements = new ArrayList<>();
        for (Iterator<Element> i = element().elementIterator(); i.hasNext();) {
            Element child = i.next();
            elements.add(child);
        }
		
		return elements;
	}
	
	public List<Element> elements(String name) {
		return elements().stream().filter(e -> e.getName().equals(name)).collect(Collectors.toList());
	}
	
	public String text() {
		return element().getTextTrim();
	}
	
	public boolean exists() {
		try {
			element();
		} catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}
	
	public String attribute(String name) {
		if (element().attribute(name) == null) {
			throw new IllegalArgumentException("Argument "+name+" does not exist at path "+String.join("/",pathParts));
		}
		return element().attributeValue(name);
	}
	
	public boolean attributeExists(String name) {
		if (element().attribute(name) == null) {
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
