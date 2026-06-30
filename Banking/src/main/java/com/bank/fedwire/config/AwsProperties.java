package com.bank.fedwire.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aws")
public class AwsProperties {

    private boolean messagingEnabled;
    private String accessKey;
    private String secretKey;
    private String region;
    private String topicArn;
    private String pacs002QueueUrl;
    private String admi002QueueUrl;
}
