package uk.ac.ebi.biosamples;

public class Messaging {

	public static final String queueToBeLoaded = "biosamples.tobeloaded";
	public static final String queueToBeIndexedSolr = "biosamples.tobeindexed.solr";
	public static final String queueToBeIndexedNeo4J = "biosamples.tobeindexed.neo4j";
	public static final String exchangeForIndexing = "biosamples.forindexing";
	public static final String exchangeForLoading = "biosamples.forloading";

}
