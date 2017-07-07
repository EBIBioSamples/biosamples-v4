package uk.ac.ebi.biosamples.model;

import org.springframework.http.HttpStatus;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "Error")
@XmlType(propOrder = { "statusCode", "message" })
public class CustomXmlError {

    public CustomXmlError() {}

    public CustomXmlError(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    private String message;
    private HttpStatus status;
    private String name = "";

    @XmlElement(name = "Message")
    String getMessage() {
        return message;
    }

    void setMessage(String message) {
        this.message = message;
    }

    void getStatus(HttpStatus statusCode) {
        this.status = statusCode;
    }

    private String getName() {
        if (name.isEmpty()) {
            this.name = this.status.name().toUpperCase().replaceAll("_"," ");
        }
        return this.name;
    }

    @XmlElement(name = "Status")
    String getStatusCode() {
        return String.format("%d - %s", this.status.value(), getName());
    }


}
