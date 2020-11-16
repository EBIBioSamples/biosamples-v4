/*
package uk.ac.ebi.biosamples.service.aws;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class BSDEmbassyService {
    public static void main(String[] args) {
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new EndpointConfiguration("https://s3.embassy.ebi.ac.uk/mybucket", "eu-west-2"))
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .withPathStyleAccessEnabled(true)
                .build();

        System.out.println("Listing objects");
        ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
                .withBucketName(""));
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
            System.out.println(" - " + objectSummary.getKey() + "  " +
                    "(size = " + objectSummary.getSize() + ")");
        }
        System.out.println();
    }
}
*/
