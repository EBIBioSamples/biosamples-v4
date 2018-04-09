package uk.ac.ebi.biosamples.solr.repo;

import java.util.ArrayList;
import java.util.Collection;

public class CursorArrayList<T> extends ArrayList<T> {
	private static final long serialVersionUID = -1828085550269612339L;
	private final String nextCursorMark;
	
	public CursorArrayList(String nextCursorMark) {
		super();
		this.nextCursorMark = nextCursorMark;
	}

	public CursorArrayList(Collection<? extends T> c, String nextCursorMark) {
		super(c);
		this.nextCursorMark = nextCursorMark;
	}

	public CursorArrayList(int initialCapacity, String nextCursorMark) {
		super(initialCapacity);
		this.nextCursorMark = nextCursorMark;
	}
	
	public String getNextCursorMark() {
		return nextCursorMark;
	}
}