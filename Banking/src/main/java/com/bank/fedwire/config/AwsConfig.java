package com.bank.fedwire.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class AwsConfig {

    private final AwsProperties awsProperties;

    @Bean
    public SnsClient snsClient() {
        log.info("Creating SNS client region={}, accessKeyLoaded={}, secretKeyLoaded={}",
                awsProperties.getRegion(),
                isLoaded(awsProperties.getAccessKey()),
                isLoaded(awsProperties.getSecretKey()));
        return SnsClient.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(awsProperties.getAccessKey(), awsProperties.getSecretKey())))
                .build();
    }

    @Bean
    public SqsClient sqsClient() {
        log.info("Creating SQS client region={}, accessKeyLoaded={}, secretKeyLoaded={}",
                awsProperties.getRegion(),
                isLoaded(awsProperties.getAccessKey()),
                isLoaded(awsProperties.getSecretKey()));
        return SqsClient.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(awsProperties.getAccessKey(), awsProperties.getSecretKey())))
                .build();
    }

    private boolean isLoaded(String value) {
        return value != null && !value.isBlank();
    }
}
