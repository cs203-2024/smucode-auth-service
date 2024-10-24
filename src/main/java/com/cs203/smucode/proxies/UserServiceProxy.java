package com.cs203.smucode.proxies;

import com.cs203.smucode.dto.UserIdentificationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
public class UserServiceProxy {
    @Value("${user.service.url}")
    private String userUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public UserServiceProxy(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void createUserProfile(UUID id, String username, String email) {
        String createProfileUrl = userUrl + "/profile/create";
        UserIdentificationDTO dto = new UserIdentificationDTO(id, username, email);

        ResponseEntity<String> response = restTemplate.postForEntity(createProfileUrl, dto, String.class);

        if (response.getStatusCode() != HttpStatus.CREATED) {
            throw new RuntimeException("Error creating user profile: " + dto);
        }
    }

    public void deleteUserProfile(UUID id, String username, String email) {
        String deleteProfileUrl = userUrl + "/profile/delete";
        UserIdentificationDTO dto = new UserIdentificationDTO(id, username, email);

        ResponseEntity<String> response = restTemplate.postForEntity(deleteProfileUrl, dto, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Error creating user profile: " + dto);
        }
    }

}