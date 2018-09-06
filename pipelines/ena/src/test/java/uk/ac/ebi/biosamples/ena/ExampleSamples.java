package uk.ac.ebi.biosamples.ena;

public class ExampleSamples {

    public static String exampleSampleXml = "<?xml version = '1.0' encoding = 'UTF-8'?><SAMPLE_SET>\n" +
            "   <SAMPLE alias=\"K510\" center_name=\"UNIBE-IG\" accession=\"ERS2295402\">\n" +
            "      <IDENTIFIERS>\n" +
            "         <PRIMARY_ID>ERS2295402</PRIMARY_ID>\n" +
            "         <SUBMITTER_ID namespace=\"UNIBE-IG\">K510</SUBMITTER_ID>\n" +
            "      </IDENTIFIERS>\n" +
            "      <TITLE>unknown/stray cat</TITLE>\n" +
            "      <SAMPLE_NAME>\n" +
            "         <TAXON_ID>9685</TAXON_ID>\n" +
            "         <SCIENTIFIC_NAME>Felis catus</SCIENTIFIC_NAME>\n" +
            "         <COMMON_NAME>domestic cat</COMMON_NAME>\n" +
            "      </SAMPLE_NAME>\n" +
            "      <DESCRIPTION>unknown/stray cat female</DESCRIPTION>\n" +
            "      <SAMPLE_ATTRIBUTES>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>ENA-CHECKLIST</TAG>\n" +
            "            <VALUE>ERC000011</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "      </SAMPLE_ATTRIBUTES>\n" +
            "   </SAMPLE>\n" +
            "</SAMPLE_SET>";

    public static String missingAliasSampleXml = "<?xml version = '1.0' encoding = 'UTF-8'?><SAMPLE_SET>\n" +
            "   <SAMPLE center_name=\"UNIBE-IG\" accession=\"ERS2295402\">\n" +
            "      <IDENTIFIERS>\n" +
            "         <PRIMARY_ID>ERS2295402</PRIMARY_ID>\n" +
            "         <SUBMITTER_ID namespace=\"UNIBE-IG\">K510</SUBMITTER_ID>\n" +
            "      </IDENTIFIERS>\n" +
            "      <TITLE>unknown/stray cat</TITLE>\n" +
            "      <SAMPLE_NAME>\n" +
            "         <TAXON_ID>9685</TAXON_ID>\n" +
            "         <SCIENTIFIC_NAME>Felis catus</SCIENTIFIC_NAME>\n" +
            "         <COMMON_NAME>domestic cat</COMMON_NAME>\n" +
            "      </SAMPLE_NAME>\n" +
            "      <DESCRIPTION>unknown/stray cat female</DESCRIPTION>\n" +
            "      <SAMPLE_ATTRIBUTES>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>ENA-CHECKLIST</TAG>\n" +
            "            <VALUE>ERC000011</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "      </SAMPLE_ATTRIBUTES>\n" +
            "   </SAMPLE>\n" +
            "</SAMPLE_SET>";

    public static String expectedModifiedMissingAliasSampleXml = "<?xml version = '1.0' encoding = 'UTF-8'?><SAMPLE_SET>\n" +
            "   <SAMPLE center_name=\"UNIBE-IG\" accession=\"ERS2295402\">\n" +
            "      <IDENTIFIERS>\n" +
            "         <PRIMARY_ID>ERS2295402</PRIMARY_ID>\n" +
            "      </IDENTIFIERS>\n" +
            "      <TITLE>unknown/stray cat</TITLE>\n" +
            "      <SAMPLE_NAME>\n" +
            "         <TAXON_ID>9685</TAXON_ID>\n" +
            "         <SCIENTIFIC_NAME>Felis catus</SCIENTIFIC_NAME>\n" +
            "         <COMMON_NAME>domestic cat</COMMON_NAME>\n" +
            "      </SAMPLE_NAME>\n" +
            "      <DESCRIPTION>unknown/stray cat female</DESCRIPTION>\n" +
            "      <SAMPLE_ATTRIBUTES>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>ENA-CHECKLIST</TAG>\n" +
            "            <VALUE>ERC000011</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "      </SAMPLE_ATTRIBUTES>\n" +
            "   </SAMPLE>\n" +
            "</SAMPLE_SET>";

    public static String missingNamespaceSampleXml = "<?xml version = '1.0' encoding = 'UTF-8'?><SAMPLE_SET>\n" +
            "   <SAMPLE alias=\"K510\" center_name=\"UNIBE-IG\" accession=\"ERS2295402\">\n" +
            "      <IDENTIFIERS>\n" +
            "         <PRIMARY_ID>ERS2295402</PRIMARY_ID>\n" +
            "         <SUBMITTER_ID>K510</SUBMITTER_ID>\n" +
            "      </IDENTIFIERS>\n" +
            "      <TITLE>unknown/stray cat</TITLE>\n" +
            "      <SAMPLE_NAME>\n" +
            "         <TAXON_ID>9685</TAXON_ID>\n" +
            "         <SCIENTIFIC_NAME>Felis catus</SCIENTIFIC_NAME>\n" +
            "         <COMMON_NAME>domestic cat</COMMON_NAME>\n" +
            "      </SAMPLE_NAME>\n" +
            "      <DESCRIPTION>unknown/stray cat female</DESCRIPTION>\n" +
            "      <SAMPLE_ATTRIBUTES>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>ENA-CHECKLIST</TAG>\n" +
            "            <VALUE>ERC000011</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "      </SAMPLE_ATTRIBUTES>\n" +
            "   </SAMPLE>\n" +
            "</SAMPLE_SET>";

    public static String emptyNamespaceSampleXml = "<?xml version = '1.0' encoding = 'UTF-8'?><SAMPLE_SET>\n" +
            "   <SAMPLE alias=\"K510\" center_name=\"UNIBE-IG\" accession=\"ERS2295402\">\n" +
            "      <IDENTIFIERS>\n" +
            "         <PRIMARY_ID>ERS2295402</PRIMARY_ID>\n" +
            "         <SUBMITTER_ID namespace=\"\">K510</SUBMITTER_ID>\n" +
            "      </IDENTIFIERS>\n" +
            "      <TITLE>unknown/stray cat</TITLE>\n" +
            "      <SAMPLE_NAME>\n" +
            "         <TAXON_ID>9685</TAXON_ID>\n" +
            "         <SCIENTIFIC_NAME>Felis catus</SCIENTIFIC_NAME>\n" +
            "         <COMMON_NAME>domestic cat</COMMON_NAME>\n" +
            "      </SAMPLE_NAME>\n" +
            "      <DESCRIPTION>unknown/stray cat female</DESCRIPTION>\n" +
            "      <SAMPLE_ATTRIBUTES>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>ENA-CHECKLIST</TAG>\n" +
            "            <VALUE>ERC000011</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "      </SAMPLE_ATTRIBUTES>\n" +
            "   </SAMPLE>\n" +
            "</SAMPLE_SET>";


    public static String expectedModifiedNcbiBrokerSampleXml = "<SAMPLE_SET>\n" +
            "   <SAMPLE center_name=\"1000G\" alias=\"NA18561\" accession=\"SRS000121\" broker_name=\"NCBI\">\n" +
            "      <IDENTIFIERS>\n" +
            "         <PRIMARY_ID>SRS000121</PRIMARY_ID>\n" +
            "         <EXTERNAL_ID namespace=\"BioSample\">SAMN00001603</EXTERNAL_ID>\n" +
            "         <EXTERNAL_ID namespace=\"Coriell\">GM18561</EXTERNAL_ID>\n" +
            "         <SUBMITTER_ID namespace=\"1000G\">NA18561</SUBMITTER_ID>\n" +
            "      </IDENTIFIERS>\n" +
            "      <TITLE>Coriell GM18561</TITLE>\n" +
            "      <SAMPLE_NAME>\n" +
            "         <TAXON_ID>9606</TAXON_ID>\n" +
            "         <SCIENTIFIC_NAME>Homo sapiens</SCIENTIFIC_NAME>\n" +
            "      </SAMPLE_NAME>\n" +
            "      <DESCRIPTION>Human HapMap individual Coriell catalog ID NA18561</DESCRIPTION>\n" +
            "      <SAMPLE_LINKS>\n" +
            "         <SAMPLE_LINK>\n" +
            "            <URL_LINK>\n" +
            "               <LABEL>dbSNP Batch ID 1061891</LABEL>\n" +
            "               <URL>http://www.ncbi.nlm.nih.gov/SNP/snp_viewBatch.cgi?sbid=1061891</URL>\n" +
            "            </URL_LINK>\n" +
            "         </SAMPLE_LINK>\n" +
            "         <SAMPLE_LINK>\n" +
            "            <URL_LINK>\n" +
            "               <LABEL>Individual record in dbSNP</LABEL>\n" +
            "               <URL>http://www.ncbi.nlm.nih.gov/projects/SNP/snp_ind.cgi?ind_id=5153</URL>\n" +
            "            </URL_LINK>\n" +
            "         </SAMPLE_LINK>\n" +
            "      </SAMPLE_LINKS>\n" +
            "      <SAMPLE_ATTRIBUTES>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>DNA-ID</TAG>\n" +
            "            <VALUE>NA18561</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>Super Population Code</TAG>\n" +
            "            <VALUE>EAS</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>population</TAG>\n" +
            "            <VALUE>CHB</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>Super Population Description</TAG>\n" +
            "            <VALUE>East Asian</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>Coriell plate</TAG>\n" +
            "            <VALUE>HAPMAPPT02</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>Coriell panel</TAG>\n" +
            "            <VALUE>MGP00017</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>Population Description</TAG>\n" +
            "            <VALUE>Han Chinese in Beijing, China</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>sex</TAG>\n" +
            "            <VALUE>male</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>HapMap sample ID</TAG>\n" +
            "            <VALUE>NA18561</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>culture_collection</TAG>\n" +
            "            <VALUE>Coriell:GM18561</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>BioSampleModel</TAG>\n" +
            "            <VALUE>HapMap</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "      </SAMPLE_ATTRIBUTES>\n" +
            "   </SAMPLE>\n" +
            "</SAMPLE_SET>";

    public static String ncbiSampleXml = "<SAMPLE_SET>\n" +
            "   <SAMPLE center_name=\"1000G\" alias=\"NA18561\" accession=\"SRS000121\">\n" +
            "      <IDENTIFIERS>\n" +
            "         <PRIMARY_ID>SRS000121</PRIMARY_ID>\n" +
            "         <EXTERNAL_ID namespace=\"BioSample\">SAMN00001603</EXTERNAL_ID>\n" +
            "         <EXTERNAL_ID namespace=\"Coriell\">GM18561</EXTERNAL_ID>\n" +
            "         <SUBMITTER_ID namespace=\"1000G\">NA18561</SUBMITTER_ID>\n" +
            "      </IDENTIFIERS>\n" +
            "      <TITLE>Coriell GM18561</TITLE>\n" +
            "      <SAMPLE_NAME>\n" +
            "         <TAXON_ID>9606</TAXON_ID>\n" +
            "         <SCIENTIFIC_NAME>Homo sapiens</SCIENTIFIC_NAME>\n" +
            "      </SAMPLE_NAME>\n" +
            "      <DESCRIPTION>Human HapMap individual Coriell catalog ID NA18561</DESCRIPTION>\n" +
            "      <SAMPLE_LINKS>\n" +
            "         <SAMPLE_LINK>\n" +
            "            <URL_LINK>\n" +
            "               <LABEL>dbSNP Batch ID 1061891</LABEL>\n" +
            "               <URL>http://www.ncbi.nlm.nih.gov/SNP/snp_viewBatch.cgi?sbid=1061891</URL>\n" +
            "            </URL_LINK>\n" +
            "         </SAMPLE_LINK>\n" +
            "         <SAMPLE_LINK>\n" +
            "            <URL_LINK>\n" +
            "               <LABEL>Individual record in dbSNP</LABEL>\n" +
            "               <URL>http://www.ncbi.nlm.nih.gov/projects/SNP/snp_ind.cgi?ind_id=5153</URL>\n" +
            "            </URL_LINK>\n" +
            "         </SAMPLE_LINK>\n" +
            "      </SAMPLE_LINKS>\n" +
            "      <SAMPLE_ATTRIBUTES>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>DNA-ID</TAG>\n" +
            "            <VALUE>NA18561</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>Super Population Code</TAG>\n" +
            "            <VALUE>EAS</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>population</TAG>\n" +
            "            <VALUE>CHB</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>Super Population Description</TAG>\n" +
            "            <VALUE>East Asian</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>Coriell plate</TAG>\n" +
            "            <VALUE>HAPMAPPT02</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>Coriell panel</TAG>\n" +
            "            <VALUE>MGP00017</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>Population Description</TAG>\n" +
            "            <VALUE>Han Chinese in Beijing, China</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>sex</TAG>\n" +
            "            <VALUE>male</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>HapMap sample ID</TAG>\n" +
            "            <VALUE>NA18561</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>culture_collection</TAG>\n" +
            "            <VALUE>Coriell:GM18561</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>BioSampleModel</TAG>\n" +
            "            <VALUE>HapMap</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "      </SAMPLE_ATTRIBUTES>\n" +
            "   </SAMPLE>\n" +
            "</SAMPLE_SET>";

    public static String ddbjSampleXml = "<?xml version = '1.0' encoding = 'UTF-8'?><SAMPLE_SET>\n" +
            "   <SAMPLE center_name=\"BioSample\" alias=\"SAMD00015737\" accession=\"DRS000378\">\n" +
            "      <IDENTIFIERS>\n" +
            "         <PRIMARY_ID>DRS000378</PRIMARY_ID>\n" +
            "         <EXTERNAL_ID namespace=\"BioSample\">SAMD00015737</EXTERNAL_ID>\n" +
            "      </IDENTIFIERS>\n" +
            "      <TITLE>Ovarian piRNAs from a female that shows W chromosome mutation linked sex differentiation deficiency (Individual No. 4-1)</TITLE>\n" +
            "      <SAMPLE_NAME>\n" +
            "         <TAXON_ID>7091</TAXON_ID>\n" +
            "         <SCIENTIFIC_NAME>Bombyx mori</SCIENTIFIC_NAME>\n" +
            "      </SAMPLE_NAME>\n" +
            "      <SAMPLE_ATTRIBUTES>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>sample_name</TAG>\n" +
            "            <VALUE>DRS000378</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>sex</TAG>\n" +
            "            <VALUE>female</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>sample comment</TAG>\n" +
            "            <VALUE>piRNA library was constructed from say 4 pupal ovary from a female that shows sex differentiation deficiency</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>dev_stage</TAG>\n" +
            "            <VALUE>4 day old pupa</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>cell type</TAG>\n" +
            "            <VALUE>ovary</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "      </SAMPLE_ATTRIBUTES>\n" +
            "   </SAMPLE>\n" +
            "</SAMPLE_SET>";

    public static String expectedModifiedDdbjBrokerSampleXml = "<?xml version = '1.0' encoding = 'UTF-8'?><SAMPLE_SET>\n" +
            "   <SAMPLE center_name=\"BioSample\" alias=\"SAMD00015737\" accession=\"DRS000378\" broker_name=\"DDBJ\">\n" +
            "      <IDENTIFIERS>\n" +
            "         <PRIMARY_ID>DRS000378</PRIMARY_ID>\n" +
            "         <EXTERNAL_ID namespace=\"BioSample\">SAMD00015737</EXTERNAL_ID>\n" +
            "      </IDENTIFIERS>\n" +
            "      <TITLE>Ovarian piRNAs from a female that shows W chromosome mutation linked sex differentiation deficiency (Individual No. 4-1)</TITLE>\n" +
            "      <SAMPLE_NAME>\n" +
            "         <TAXON_ID>7091</TAXON_ID>\n" +
            "         <SCIENTIFIC_NAME>Bombyx mori</SCIENTIFIC_NAME>\n" +
            "      </SAMPLE_NAME>\n" +
            "      <SAMPLE_ATTRIBUTES>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>sample_name</TAG>\n" +
            "            <VALUE>DRS000378</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>sex</TAG>\n" +
            "            <VALUE>female</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>sample comment</TAG>\n" +
            "            <VALUE>piRNA library was constructed from say 4 pupal ovary from a female that shows sex differentiation deficiency</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>dev_stage</TAG>\n" +
            "            <VALUE>4 day old pupa</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>cell type</TAG>\n" +
            "            <VALUE>ovary</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "      </SAMPLE_ATTRIBUTES>\n" +
            "   </SAMPLE>\n" +
            "</SAMPLE_SET>";

    public static String expectedModifiedNcbiLinksRemoved = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "\n" +
            "<SAMPLE_SET> \n" +
            "  <SAMPLE center_name=\"1000G\" alias=\"NA18561\" accession=\"SRS000121\"> \n" +
            "    <IDENTIFIERS> \n" +
            "      <PRIMARY_ID>SRS000121</PRIMARY_ID>  \n" +
            "      <EXTERNAL_ID namespace=\"BioSample\">SAMN00001603</EXTERNAL_ID>  \n" +
            "      <EXTERNAL_ID namespace=\"Coriell\">GM18561</EXTERNAL_ID>  \n" +
            "      <SUBMITTER_ID namespace=\"1000G\">NA18561</SUBMITTER_ID> \n" +
            "    </IDENTIFIERS>  \n" +
            "    <TITLE>Coriell GM18561</TITLE>  \n" +
            "    <SAMPLE_NAME> \n" +
            "      <TAXON_ID>9606</TAXON_ID>  \n" +
            "      <SCIENTIFIC_NAME>Homo sapiens</SCIENTIFIC_NAME> \n" +
            "    </SAMPLE_NAME>  \n" +
            "    <DESCRIPTION>Human HapMap individual Coriell catalog ID NA18561</DESCRIPTION>  \n" +
            "    <SAMPLE_LINKS></SAMPLE_LINKS>    \n" +
            "    <SAMPLE_ATTRIBUTES> \n" +
            "      <SAMPLE_ATTRIBUTE> \n" +
            "        <TAG>DNA-ID</TAG>  \n" +
            "        <VALUE>NA18561</VALUE> \n" +
            "      </SAMPLE_ATTRIBUTE>  \n" +
            "      <SAMPLE_ATTRIBUTE> \n" +
            "        <TAG>Super Population Code</TAG>  \n" +
            "        <VALUE>EAS</VALUE> \n" +
            "      </SAMPLE_ATTRIBUTE>  \n" +
            "      <SAMPLE_ATTRIBUTE> \n" +
            "        <TAG>population</TAG>  \n" +
            "        <VALUE>CHB</VALUE> \n" +
            "      </SAMPLE_ATTRIBUTE>  \n" +
            "      <SAMPLE_ATTRIBUTE> \n" +
            "        <TAG>Super Population Description</TAG>  \n" +
            "        <VALUE>East Asian</VALUE> \n" +
            "      </SAMPLE_ATTRIBUTE>  \n" +
            "      <SAMPLE_ATTRIBUTE> \n" +
            "        <TAG>Coriell plate</TAG>  \n" +
            "        <VALUE>HAPMAPPT02</VALUE> \n" +
            "      </SAMPLE_ATTRIBUTE>  \n" +
            "      <SAMPLE_ATTRIBUTE> \n" +
            "        <TAG>Coriell panel</TAG>  \n" +
            "        <VALUE>MGP00017</VALUE> \n" +
            "      </SAMPLE_ATTRIBUTE>  \n" +
            "      <SAMPLE_ATTRIBUTE> \n" +
            "        <TAG>Population Description</TAG>  \n" +
            "        <VALUE>Han Chinese in Beijing, China</VALUE> \n" +
            "      </SAMPLE_ATTRIBUTE>  \n" +
            "      <SAMPLE_ATTRIBUTE> \n" +
            "        <TAG>sex</TAG>  \n" +
            "        <VALUE>male</VALUE> \n" +
            "      </SAMPLE_ATTRIBUTE>  \n" +
            "      <SAMPLE_ATTRIBUTE> \n" +
            "        <TAG>HapMap sample ID</TAG>  \n" +
            "        <VALUE>NA18561</VALUE> \n" +
            "      </SAMPLE_ATTRIBUTE>  \n" +
            "      <SAMPLE_ATTRIBUTE> \n" +
            "        <TAG>culture_collection</TAG>  \n" +
            "        <VALUE>Coriell:GM18561</VALUE> \n" +
            "      </SAMPLE_ATTRIBUTE>  \n" +
            "      <SAMPLE_ATTRIBUTE> \n" +
            "        <TAG>BioSampleModel</TAG>  \n" +
            "        <VALUE>HapMap</VALUE> \n" +
            "      </SAMPLE_ATTRIBUTE> \n" +
            "    </SAMPLE_ATTRIBUTES> \n" +
            "  </SAMPLE> \n" +
            "</SAMPLE_SET>";
}