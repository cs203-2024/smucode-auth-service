package com.cs203.smucode.proxies;

import com.cs203.smucode.dto.UserIdentificationDTO;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;


@Component
@FeignClient(name = "user-service", url="${user.service.url}")
public interface UserServiceProxy {
    @PostMapping("/profile/create")
    void createUserProfile(@Valid UserIdentificationDTO userIdentificationDTO);

    @PostMapping("/profile/delete")
    void deleteUserProfile(@Valid UserIdentificationDTO userIdentificationDTO);
}