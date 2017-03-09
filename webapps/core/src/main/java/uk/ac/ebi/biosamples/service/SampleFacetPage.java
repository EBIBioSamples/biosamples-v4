package uk.ac.ebi.biosamples.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.PivotField;
import org.springframework.data.solr.core.query.result.FacetEntry;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.FacetPivotFieldEntry;
import org.springframework.data.solr.core.query.result.FacetQueryEntry;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.solr.model.SolrSample;

/**
 * Class that wraps another FacetPage to dynamically convert the objects returned from the 
 * page but allowing facets and page information passed through unaltered 
 * 
 * @author faulcon
 *
 * @param <Sample> class of the original FacetPage
 * @param <S> class of the target objects
 */
public class SampleFacetPage implements FacetPage<Sample> {

	private final FacetPage<SolrSample> originalFacetPage;
	private List<Sample> content = null;
	
	private SampleFacetPage(FacetPage<SolrSample> originalFacetPage) {
		this.originalFacetPage = originalFacetPage;
	}
		
	@Override
	public Page<FacetFieldEntry> getFacetResultPage(String fieldname) {
		return originalFacetPage.getFacetResultPage(fieldname);
	}

	@Override
	public Page<FacetFieldEntry> getFacetResultPage(Field field) {
		return originalFacetPage.getFacetResultPage(field);
	}

	@Override
	public Page<FacetFieldEntry> getRangeFacetResultPage(String fieldname) {
		return originalFacetPage.getFacetResultPage(fieldname);
	}

	@Override
	public Page<FacetFieldEntry> getRangeFacetResultPage(Field field) {
		return originalFacetPage.getFacetResultPage(field);
	}

	@Override
	public List<FacetPivotFieldEntry> getPivot(String fieldName) {
		return originalFacetPage.getPivot(fieldName);
	}

	@Override
	public List<FacetPivotFieldEntry> getPivot(PivotField field) {
		return originalFacetPage.getPivot(field);
	}

	@Override
	public Collection<Page<FacetFieldEntry>> getFacetResultPages() {
		return originalFacetPage.getFacetResultPages();
	}

	@Override
	public Page<FacetQueryEntry> getFacetQueryResult() {
		return originalFacetPage.getFacetQueryResult();
	}

	@Override
	public Collection<Field> getFacetFields() {
		return originalFacetPage.getFacetFields();
	}

	@Override
	public Collection<PivotField> getFacetPivotFields() {
		return originalFacetPage.getFacetPivotFields();
	}

	@Override
	public Collection<Page<? extends FacetEntry>> getAllFacets() {
		return originalFacetPage.getAllFacets();
	}

	@Override
	public int getTotalPages() {
		return originalFacetPage.getTotalPages();
	}

	@Override
	public long getTotalElements() {
		return originalFacetPage.getTotalPages();
	}

	@Override
	public <S> Page<S> map(Converter<? super Sample, ? extends S> converter) {
		throw new IllegalArgumentException("Not implemented");
	}

	@Override
	public int getNumber() {
		return originalFacetPage.getNumber();
	}

	@Override
	public int getSize() {
		return originalFacetPage.getSize();
	}

	@Override
	public int getNumberOfElements() {
		return originalFacetPage.getNumberOfElements();
	}

	@Override
	public List<Sample> getContent() {
		return Collections.unmodifiableList(this.content);
	}

	@Override
	public boolean hasContent() {
		return originalFacetPage.hasContent();
	}

	@Override
	public Sort getSort() {
		return originalFacetPage.getSort();
	}

	@Override
	public boolean isFirst() {
		return originalFacetPage.isFirst();
	}

	@Override
	public boolean isLast() {
		return originalFacetPage.isLast();
	}

	@Override
	public boolean hasNext() {
		return originalFacetPage.hasNext();
	}

	@Override
	public boolean hasPrevious() {
		return originalFacetPage.hasPrevious();
	}

	@Override
	public Pageable nextPageable() {
		return originalFacetPage.nextPageable();
	}

	@Override
	public Pageable previousPageable() {
		return originalFacetPage.previousPageable();
	}

	@Override
	public Iterator<Sample> iterator() {
		return getContent().iterator();
	}
	
	/**
	 * Build the ConverterFacetPage and use the converter to populate the content
	 * when it is built rather than later to avoid session issues.	 * 
	 * 
	 * @param originalFacetPage
	 * @param converter
	 * @return
	 */
	public static SampleFacetPage build(FacetPage<SolrSample> originalFacetPage, SampleService sampleService) {
		SampleFacetPage converterFacetPage = new SampleFacetPage(originalFacetPage);
		List<Sample> content = new ArrayList<>(originalFacetPage.getContent().size());
		for (SolrSample orig : originalFacetPage.getContent()) {
			content.add(sampleService.fetch(orig.getAccession()));
		}
		converterFacetPage.content = content;		
		return converterFacetPage;
	}

}
