package uk.ac.ebi.biosamples.migration;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.ComparisonFormatter;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.DefaultComparisonFormatter;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.builder.Input;
import org.xmlunit.input.CommentLessSource;
import org.xmlunit.input.WhitespaceNormalizedSource;

import com.google.common.collect.Sets;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.XmlSampleToSampleConverter;

class AccessionComparisonCallable implements Callable<Void> {
	private final RestTemplate restTemplate;
	private final String oldUrl;
	private final String newUrl;
	private final Queue<String> bothQueue;
	private final AtomicBoolean bothFlag;
	private final XmlSampleToSampleConverter xmlToSampleConverter;
	private final boolean compare;

	private final Logger log = LoggerFactory.getLogger(getClass());

	public AccessionComparisonCallable(RestTemplate restTemplate, String oldUrl, String newUrl, Queue<String> bothQueue,
			AtomicBoolean bothFlag, XmlSampleToSampleConverter xmlToSampleConverter, boolean compare) {
		this.restTemplate = restTemplate;
		this.oldUrl = oldUrl;
		this.newUrl = newUrl;
		this.bothQueue = bothQueue;
		this.bothFlag = bothFlag;
		this.xmlToSampleConverter = xmlToSampleConverter;
		this.compare = compare;
	}

	@Override
	public Void call() throws Exception {
		log.info("Started");
		log.info("oldUrl = "+oldUrl);
		log.info("newUrl = "+newUrl);
		log.info("compare = "+compare);
		SortedSet<String> problemAccessions = new TreeSet<>();

		while (!bothFlag.get() || !bothQueue.isEmpty()) {
			String accession = bothQueue.poll();
			if (accession != null) {
				log.info("Comparing accession "+ accession);
				if (compare) {
					try {
						compare(accession);
					} catch (RestClientException e) {
						//there was a rest error, log it and continue
						problemAccessions.add(accession);
						log.error("Problem accessing "+accession, e);
					}
				}
			} else {
				Thread.sleep(100);
			}
		}
		for (String accession : problemAccessions) {
			log.error("Problem accessing "+accession);
		}
		log.info("Finished AccessionComparisonCallable.call(");
		return null;
	}

	public void compare(String accession) {
		log.info("Comparing accession " + accession);

		UriComponentsBuilder oldUriComponentBuilder = UriComponentsBuilder.fromUriString(oldUrl);
		UriComponentsBuilder newUriComponentBuilder = UriComponentsBuilder.fromUriString(newUrl);

		ComparisonFormatter comparisonFormatter = new DefaultComparisonFormatter();

		URI oldUri = oldUriComponentBuilder.cloneBuilder().pathSegment(accession).build().toUri();
		URI newUri = newUriComponentBuilder.cloneBuilder().pathSegment(accession).build().toUri();
		String oldDocument = getDocument(oldUri);
		String newDocument = getDocument(newUri);

/*		
		<BioSample xmlns="http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="SAMEA19131418" submissionReleaseDate="2017-03-29T23:00:00+00:00" submissionUpdateDate="2017-03-30T20:59:36+00:00" xsi:schemaLocation="http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0 http://www.ebi.ac.uk/biosamples/assets/xsd/v1.0/BioSDSchema.xsd">
		  <Property characteristic="true" class="Sample Name" comment="false" type="STRING">
		    <QualifiedValue>
		      <Value>ERS1463623</Value>
		    </QualifiedValue>
		  </Property>
		  <Property characteristic="false" class="synonym" comment="true" type="STRING">
		    <QualifiedValue>
		      <Value>7d9ac5c0-b6e5-11e6-b226-68b599768938</Value>
		    </QualifiedValue>
		    <QualifiedValue>
		      <Value>MUS MUSCULUS</Value>
		    </QualifiedValue>
		  </Property>
		  <Property characteristic="false" class="Organism" comment="false" type="STRING">
		    <QualifiedValue>
		      <Value>Mus musculus</Value>
		      <TermSourceREF>
		        <Name>NCBI Taxonomy</Name>
		        <Description/>
		        <URI>http://www.ncbi.nlm.nih.gov/taxonomy/</URI>
		        <Version/>
		        <TermSourceID>10090</TermSourceID>
		      </TermSourceREF>
		    </QualifiedValue>
		  </Property>
		  <Property characteristic="true" class="unknown" comment="false" type="STRING">
		    <QualifiedValue>
		      <Value>strain</Value>
		    </QualifiedValue>
		  </Property>
		  <Property characteristic="false" class="secondary description" comment="true" type="STRING">
		    <QualifiedValue>
		      <Value>BLOOD CELLS</Value>
		    </QualifiedValue>
		  </Property>
		  <Property characteristic="true" class="Species" comment="false" type="STRING">
		    <QualifiedValue>
		      <Value>Mus musculus</Value>
		    </QualifiedValue>
		  </Property>
		  <Database>
		    <Name>ENA</Name>
		    <ID>ERS1463623</ID>
		    <URI>http://www.ebi.ac.uk/ena/data/view/ERS1463623</URI>
		  </Database>
		  <Database>
		    <Name>ENA</Name>
		    <ID>SAMEA19131418</ID>
		    <URI>http://www.ebi.ac.uk/ena/data/view/SAMEA19131418</URI>
		  </Database>
		</BioSample>

*/
		
		/*
		Diff diff = DiffBuilder
				.compare(new WhitespaceNormalizedSource(new CommentLessSource(Input.fromString(oldDocument).build())))
				.withTest(new WhitespaceNormalizedSource(new CommentLessSource(Input.fromString(newDocument).build())))
		        .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
				.checkForSimilar()
				.build();
		
		if (diff.hasDifferences()) {
			List<Difference> differences = new ArrayList<>();
			for (Difference difference : diff.getDifferences()) {
				if (difference.getResult() == ComparisonResult.DIFFERENT) {
					differences.add(difference);
				}
			}

			log.info("Found " + differences.size() + " differences on " + accession);
			for (Difference difference : differences) {
				log.info(String.join(" ", "Difference on", accession, comparisonFormatter.getDescription(difference.getComparison())));
			}
		}
		*/
		

		SAXReader saxReader = new SAXReader();
		org.dom4j.Document doc;
		try {
			doc = saxReader.read(new StringReader(oldDocument));
		} catch (DocumentException e) {
			throw new HttpMessageNotReadableException("error parsing xml", e);
		}
		Sample oldSample = xmlToSampleConverter.convert(doc.getRootElement());
		try {
			doc = saxReader.read(new StringReader(newDocument));
		} catch (DocumentException e) {
			throw new HttpMessageNotReadableException("error parsing xml", e);
		}
		Sample newSample = xmlToSampleConverter.convert(doc.getRootElement());
		
		if (!oldSample.getAccession().equals(newSample.getAccession())) {
			log.warn("Difference on "+accession+" of accession between '"+oldSample.getAccession()+"' and '"+newSample.getAccession()+"'");
		}
		if (!oldSample.getName().equals(newSample.getName())) {
			log.warn("Difference on "+accession+" of name between '"+oldSample.getName()+"' and '"+newSample.getName()+"'");
		}
				
		if (Math.abs(ChronoUnit.DAYS.between(oldSample.getUpdate(), newSample.getUpdate())) > 1) {
			//TODO disabled for moment
			//log.warn("Difference on "+accession+" of update date between '"+oldSample.getUpdate()+"' and '"+newSample.getUpdate()+"'");
		}
		if (Math.abs(ChronoUnit.DAYS.between(oldSample.getRelease(), newSample.getRelease())) > 1) {
			log.warn("Difference on "+accession+" of release date between '"+oldSample.getRelease()+"' and '"+newSample.getRelease()+"'");
		}
		
		Set<String> oldAttributeTypes = oldSample.getAttributes().stream().map(a -> a.getType()).collect(Collectors.toSet());
		Set<String> newAttributeTypes = newSample.getAttributes().stream().map(a -> a.getType()).collect(Collectors.toSet());		
		for (String attributeType : Sets.difference(oldAttributeTypes, newAttributeTypes)) {
			log.warn("Difference on "+accession+" of attribute '"+attributeType+"' in old only");			
		}
		for (String attributeType : Sets.difference(newAttributeTypes, oldAttributeTypes)) {
			log.warn("Difference on "+accession+" of attribute '"+attributeType+"' in new only");			
		}
		
		for (String attributeType : Sets.intersection(oldAttributeTypes, newAttributeTypes)) {
			List<Attribute> oldAttributes = oldSample.getAttributes().stream()
					.filter(a -> attributeType.equals(a.getType())).collect(Collectors.toList());
			Collections.sort(oldAttributes);
			List<Attribute> newAttributes = newSample.getAttributes().stream()
					.filter(a -> attributeType.equals(a.getType())).collect(Collectors.toList());
			Collections.sort(newAttributes);			
			
			SortedMap<String,SortedMap<String,String>> oldUnits = new TreeMap<>();
			SortedMap<String,SortedMap<String,SortedSet<String>>> oldIris = new TreeMap<>();
			SortedMap<String,SortedMap<String,String>> newUnits = new TreeMap<>();
			SortedMap<String,SortedMap<String,SortedSet<String>>> newIris = new TreeMap<>();
			
			if (oldAttributes.size() != newAttributes.size()) {
				log.warn("Difference on "+accession+" of attribute '"+attributeType+"' has "+oldAttributes.size()+" values in old and "+newAttributes.size()+" values in new");		
			} else {
				for (int i = 0; i < oldAttributes.size(); i++) {
					Attribute oldAttribute = oldAttributes.get(i);
					Attribute newAttribute = newAttributes.get(i);
					
					//TODO finish me
					
					if (!oldUnits.containsKey(oldAttribute.getType())) {
						oldUnits.put(oldAttribute.getType(), new TreeMap<>());
					}
					oldUnits.get(oldAttribute.getType()).put(oldAttribute.getValue(), oldAttribute.getUnit());
					if (!newUnits.containsKey(newAttribute.getType())) {
						newUnits.put(newAttribute.getType(), new TreeMap<>());
					}
					newUnits.get(newAttribute.getType()).put(newAttribute.getValue(), newAttribute.getUnit());
					
					if (!oldIris.containsKey(oldAttribute.getType())) {
						oldIris.put(oldAttribute.getType(), new TreeMap<>());
					}
					oldIris.get(oldAttribute.getType()).put(oldAttribute.getValue(), oldAttribute.getIri());
					if (!newIris.containsKey(newAttribute.getType())) {
						newIris.put(newAttribute.getType(), new TreeMap<>());
					}
					newIris.get(newAttribute.getType()).put(newAttribute.getValue(), newAttribute.getIri());
					
					//compare values
					if (!oldAttribute.getType().equals(newAttribute.getType())) {
						log.warn("Difference on "+accession+" of attribute '"+attributeType+"' between '"+oldAttribute.getType()+"' and '"+newAttribute.getType()+"'");
					}
					//compare units
					if (oldAttribute.getUnit() != null && newAttribute.getUnit() != null
							&& !oldAttribute.getUnit().equals(newAttribute.getUnit()) ) {
						log.warn("Difference on "+accession+" of attribute '"+attributeType+"' between units '"+oldAttribute.getUnit()+"' and '"+newAttribute.getUnit()+"'");
					}
					//compare iris
					if (!oldAttribute.getIri().equals(newAttribute.getIri())) {
						if (oldAttribute.getIri().size() < newAttribute.getIri().size()) {
							log.warn("Difference on "+accession+" of attribute '"+attributeType+"' between iris '"+oldAttribute.getIri()+"' and '"+newAttribute.getIri()+"'");
						} else if (oldAttribute.getIri().size() > newAttribute.getIri().size()) {
							log.warn("Difference on "+accession+" of attribute '"+attributeType+"' between iris '"+oldAttribute.getIri()+"' and '"+newAttribute.getIri()+"'");
						} else {
							Iterator<String> thisIt = oldAttribute.getIri().iterator();
							Iterator<String> otherIt = newAttribute.getIri().iterator();
							while (thisIt.hasNext() && otherIt.hasNext()) {
								int val = thisIt.next().compareTo(otherIt.next());
								if (val != 0) {
									log.warn("Difference on "+accession+" of attribute '"+attributeType+"' between iris '"+oldAttribute.getIri()+"' and '"+newAttribute.getIri()+"'");
								}
							}
						}
					}
				}
			}
		}
		
	}

	public String getDocument(URI uri) {
		//log.info("Getting " + uri);
		ResponseEntity<String> response;
		try {
			response = restTemplate.getForEntity(uri, String.class);
		} catch (RestClientException e) {
			log.error("Problem accessing " + uri, e);
			throw e;
		}
		String xmlString = response.getBody();
		xmlString = toPrettyString(xmlString, 2);
		//System.out.println(xmlString);
		// SAXReader reader = new SAXReader();
		// Document xml = reader.read(new StringReader(xmlString));
		return xmlString;
	}

	public String toPrettyString(String xml, int indent) {
		try {
			// Turn xml string into a document
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
					.parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));

			// Remove whitespaces outside tags
			document.normalize();
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']", document,
					XPathConstants.NODESET);

			for (int i = 0; i < nodeList.getLength(); ++i) {
				Node node = nodeList.item(i);
				node.getParentNode().removeChild(node);
			}
			
			cleanChildNodes(document.getDocumentElement());
			sortChildNodes(document.getDocumentElement());
			

			// Setup pretty print options
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", indent);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			// Return pretty print xml string
			StringWriter stringWriter = new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
			return stringWriter.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void cleanChildNodes(Node node) {
		//no child nodes, no need to sort it
		if (node.getChildNodes().getLength() > 0) {			
			//to clean it, have to put all the children nodes into a list and clean the list
			List<Node> nodes = new ArrayList<>();
			for (int i = 0; i < node.getChildNodes().getLength(); i++) {
				Node child = node.getChildNodes().item(i);
				//Recursively ensure all grandchildren are cleaned
				cleanChildNodes(child);
				
				nodes.add(child);
			}			
			Collections.sort(nodes, new NodeComparator());			
			for (Node child : nodes) {
				node.removeChild(child);
				node.appendChild(child);
			}
		}
		//remove "characteristic" and "comment" attributes from "Property" elements
		if (node.getNodeName().equals("Property")) {
			if (node.getAttributes().getNamedItem("characteristic") != null) {
				node.getAttributes().removeNamedItem("characteristic");
			}
			if (node.getAttributes().getNamedItem("comment") != null) {
				node.getAttributes().removeNamedItem("comment");
			}
		}
	}

	public void sortChildNodes(Node node) {
		//no child nodes, no need to sort it
		if (node.getChildNodes().getLength() > 0) {			
			//to sort it, have to put all the children nodes into a list and sort the list
			List<Node> nodes = new ArrayList<>();
			for (int i = 0; i < node.getChildNodes().getLength(); i++) {
				Node child = node.getChildNodes().item(i);
				//Recursively ensure all grandchildren are sorted
				sortChildNodes(child);
				
				nodes.add(child);
			}			
			Collections.sort(nodes, new NodeComparator());			
			for (Node child : nodes) {
				node.removeChild(child);
				node.appendChild(child);
			}
		}
	}
	
	private static class NodeComparator implements Comparator<Node> {
		
		private NamedNodeMapComparator namedNodeMapComparator = new NamedNodeMapComparator();
		
		@Override
		public int compare(Node a, Node b) {					
			if (a.getNodeName().equals(b.getNodeName())) {
				return namedNodeMapComparator.compare(a.getAttributes(), b.getAttributes());
			} else {					
				return a.getNodeName().compareTo(b.getNodeName());
			}
		}		
	}
	
	private static class NamedNodeMapComparator implements Comparator<NamedNodeMap> {

		private final Logger log = LoggerFactory.getLogger(getClass());
		
		@Override
		public int compare(NamedNodeMap a, NamedNodeMap b) {
			if (a.getLength() == b.getLength()) {
				for (int i = 0; i < a.getLength(); i++) {
					String aName = a.item(i).getNodeName();
					String bName = b.item(i).getNodeName();
					log.trace("Comparing "+aName+" with "+bName);					
					if (!aName.equals(bName)) {
						return aName.compareTo(bName);
					}

					String aValue = a.item(i).getNodeValue();
					String bValue = b.item(i).getNodeValue();	
					log.trace("Comparing "+aValue+" with "+bValue);
					if (!aValue.equals(bValue)) {
						return aValue.compareTo(bValue);
					}
					
				}
				//all the same, return same
				return 0;
			} else {
				return Integer.compare(a.getLength(), b.getLength());
			}
		}
		
	}
}