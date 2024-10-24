package com.cs203.smucode.utils;

import com.nimbusds.jose.jwk.RSAKey;

import java.time.Instant;
import java.util.stream.Collectors;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * @author: gav
 * @version: 1.1
 * @since: 24-09-18
 * @description: Utility class for JWT operations
 */
@Getter
@Component
public class JWTUtil {

    @Value("${jwt.expiration}")
    private long expirationTime;

    private final RSAKey rsaKey;

    private final JwtEncoder jwtEncoder;

    @Autowired
    public JWTUtil(JwtEncoder jwtEncoder, RSAKey rsaKey) {
        this.jwtEncoder = jwtEncoder;
        this.rsaKey = rsaKey;
    }

    /**
     * Generates a JWT token post-authentication.
     *
     * @param auth the Authentication object containing user details
     * @return a String representation of the signed JWT token (for Bearer)
     */
    public String generateToken(Authentication auth) {
        if (!(auth.getPrincipal() instanceof UserDetails userDetails)) {
            throw new AuthenticationServiceException("UserDetails not found, got " + auth.getPrincipal().getClass().getName());
        }

        try {
            JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                    .issuer("smucode-auth-service")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(expirationTime))
                    .subject(auth.getName())
                    .claim(
                            "scope",
                            userDetails
                                    .getAuthorities()
                                    .stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .collect(Collectors.joining(" "))
                    )
                    .build();

            return jwtEncoder.encode(JwtEncoderParameters.from(claimsSet)).getTokenValue();
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong", e);
        }
    }
}
