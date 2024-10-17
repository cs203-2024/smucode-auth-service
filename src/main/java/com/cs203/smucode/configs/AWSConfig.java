package com.cs203.smucode.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

/**
 * @author gav
 * @since 2024-10-12
 */
@Configuration
public class AWSConfig {

    // @Value("${aws.role.arn}")
    // private String roleArn;

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        return DefaultCredentialsProvider.create();
        // if (roleArn != null && !roleArn.isEmpty()) {
        //     logger.info("inside");
        //     StsClient stsClient = StsClient.builder()
        //             .region(Region.AP_SOUTHEAST_1)
        //             .build();

        //     AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
        //             .roleArn(roleArn)
        //             .roleSessionName("smucode-user-session")
        //             .build();
        //     logger.info(roleArn);

        //     return StsAssumeRoleCredentialsProvider.builder()
        //             .stsClient(stsClient)
        //             .refreshRequest(assumeRoleRequest)
        //             .build();
        // }
    }
}
