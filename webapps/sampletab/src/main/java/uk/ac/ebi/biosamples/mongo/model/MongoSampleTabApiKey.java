package uk.ac.ebi.biosamples.mongo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Document
public class MongoSampleTabApiKey {

	@Id
	private String apiKey;	
	private String userName;
	private String publicEmail;
	private String publicUrl;
	private String contactName;
	private String contactEmail;
	private String aapDomain;
	
	private MongoSampleTabApiKey(String apiKey, String userName, String publicEmail, String publicUrl,
			String contactName, String contactEmail, String aapDomain) {
		super();
		this.apiKey = apiKey;
		this.userName = userName;
		this.publicEmail = publicEmail;
		this.publicUrl = publicUrl;
		this.contactName = contactName;
		this.contactEmail = contactEmail;
		this.aapDomain = aapDomain;
	}

	public String getApiKey() {
		return apiKey;
	}
	
	public String getUserName() {
		return userName;
	}
	
	public String getPublicEmail() {
		return publicEmail;
	}
	
	public String getPublicUrl() {
		return publicUrl;
	}
	
	public String getContactName() {
		return contactName;
	}
	
	public String getContactEmail() {
		return contactEmail;
	}
	
	public String getAapDomain() {
		return aapDomain;
	}
	
	public static MongoSampleTabApiKey build(String apiKey, String userName, String publicEmail, String publicUrl,
			String contactName, String contactEmail, String aapDomain) {
		return new MongoSampleTabApiKey(apiKey, userName, publicEmail, publicUrl, contactName, contactEmail, aapDomain);
	}
	
}