package uk.ac.ebi.biosamples.model;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonDeserialize(using = TicketDeserializer.class)
public class ENAHtsgetTicket {

        private String accession;
        private List<String> ftpLinks;
        private String md5Hashs;
        private String format;

        public ENAHtsgetTicket(){
            ftpLinks = new ArrayList<>();
        }
        public String getAccession() {
            return accession;
        }

        public void setAccession(String accession) {
            this.accession = accession;
        }

        public List<String> getFtpLinks() {
            return ftpLinks;
        }

        public void addFtpLink(String ftpLink) {
            this.ftpLinks .add(ftpLink);
        }

        public String getMd5Hash() {
            return md5Hashs;
        }

        public void setMd5Hash(String hash) {
            this.md5Hashs = hash;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ENAHtsgetTicket that = (ENAHtsgetTicket) o;
            return  Objects.equals(accession,that.accession) &&
                    Objects.equals(ftpLinks, that.ftpLinks) &&
                    Objects.equals(md5Hashs, that.md5Hashs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ftpLinks, md5Hashs);
        }



}
