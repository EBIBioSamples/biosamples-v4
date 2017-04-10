# Legacy XML Developer Journal

## Export RequestQuery 
In order to export the XML for the RequestQuery I found
that the easiest thing to do was to reuse
the BioSamples V3 way of doing this.
Using the [ResultQuery](../webapps/legacyxml/src/main/java/uk/ac/ebi/biosamples/model/ResultQuery.java),
and rendering the content as String I was able to 
bypass all the problems of creating a specific converter
for the class.

## Support groupsamples endpoint
The groupsamples endpoint was a bit complicated to support. The biggest problem is that we don't have an
easy way to say "Find all the samples that are derived from another sample", while in the past instead
we had the ability to get all samples from a group.
Probably in the future such functionality will be handled better, at the moment I'm returning 
samples matching the query.
