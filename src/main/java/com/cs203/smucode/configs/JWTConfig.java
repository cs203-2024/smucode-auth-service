package com.cs203.smucode.configs;

import com.cs203.smucode.utils.AWSUtil;
import com.nimbusds.jose.crypto.RSASSASigner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JWTConfig {
    /**
     * Initializes by setting up the RSA signer with a private key fetched from AWS Secrets Manager.
     *
     * @throws InvalidKeySpecException if the private key specification is invalid
     * @throws NoSuchAlgorithmException if the RSA algorithm is not available
     */
    @Bean
    public RSASSASigner rsaSigner(AWSUtil awsUtil)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        String privateKeyStr = awsUtil.getValueFromSecretsManager(
                "JWTPrivateKey"
        );

        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyStr);

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
                privateKeyBytes
        );
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        return new RSASSASigner(privateKey);
    }

}
