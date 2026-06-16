package me.alinizamani.byline.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${app.aws.credentials.access-key}")
    private String accessKey;

    @Value("${app.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${app.aws.region}")
    private String region;

    @Value("${app.aws.endpoint}")
    private String endpoint;

    @Bean
    public S3Client s3Client(){

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))   // points SDK to MinIO
                .forcePathStyle(true)
                .build();
    }
}
