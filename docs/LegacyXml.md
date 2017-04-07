# Legacy XML Developer Journal

## Export RequestQuery 
In order to export the XML for the RequestQuery I found
that the easiest thing to do was to reuse
the BioSamples V3 way of doing this.
Using the [ResultQuery](../webapps/legacyxml/src/main/java/uk/ac/ebi/biosamples/model/ResultQuery.java),
and rendering the content as String I was able to 
bypass all the problems of creating a specific converter
for the class.