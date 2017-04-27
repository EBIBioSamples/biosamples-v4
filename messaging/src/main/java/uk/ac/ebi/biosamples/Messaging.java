package uk.ac.ebi.biosamples;

public class Messaging {

	public static final String queueToBeIndexedSolr = "biosamples.tobeindexed.solr";
	public static final String queueToBeIndexedNeo4J = "biosamples.tobeindexed.neo4j";
	
	public static final String queueToBeCurated = "biosamples.tobecurated";	
	
	public static final String exchangeForIndexing = "biosamples.forindexing";
	public static final String exchangeForIndexingSolr = "biosamples.forindexing.solr";
	public static final String exchangeForIndexingNeo4J = "biosamples.forindexing.neo4j";
	public static final String exchangeForCuration = "biosamples.forcuration";

}
