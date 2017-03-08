package uk.ac.ebi.biosamples.solr.repo;

import java.util.ArrayList;
import java.util.Collection;
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

/**
 * Class that wraps another FacetPage to dynamically convert the objects returned from the 
 * page but allowing facets and page information passed through unaltered 
 * 
 * @author faulcon
 *
 * @param <T> class of the original FacetPage
 * @param <S> class of the target objects
 */
public class ConverterFacetPage<T, U> implements FacetPage<T> {

	private final FacetPage<U> originalFacetPage;
	private final Converter<U,T> converter;
	private List<T> content = null;
	
	public ConverterFacetPage(FacetPage<U> originalFacetPage, Converter<U,T> converter) {
		this.converter = converter;
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
	public <S> Page<S> map(Converter<? super T, ? extends S> converter) {
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
	public List<T> getContent() {
		if (this.content == null && originalFacetPage.getContent() != null) { 
			synchronized(this) {
				List<U> orignalContent = originalFacetPage.getContent();
				List<T> content = new ArrayList<>(orignalContent.size());
				for (U orig : orignalContent) {
					content.add(converter.convert(orig));
				}
				this.content = content;
			}
		}
		return this.content;
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
	public Iterator<T> iterator() {
		return getContent().iterator();
	}

}
