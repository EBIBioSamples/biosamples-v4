package uk.ac.ebi.biosamples;

public class Messaging {

	
	public static final String queueToBeIndexedSolr = "biosamples.tobeindexed.solr";
	public static final String exchangeForIndexingSolr = "biosamples.forindexing.solr";
	public static final String queueRetryDeadLetter = "biosamples.deadletter.retry";
	public static final String exchangeDeadLetter = "biosamples.deadletter";

}
