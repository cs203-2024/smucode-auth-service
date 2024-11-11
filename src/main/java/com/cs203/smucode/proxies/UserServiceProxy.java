package com.cs203.smucode.proxies;

import com.cs203.smucode.dto.UserIdentificationDTO;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(name = "user-service")
public interface UserServiceProxy {
    @PostMapping("/profile/create")
    void createUserProfile(@RequestBody @Valid UserIdentificationDTO userIdentificationDTO);

    @PostMapping("/profile/delete")
    void deleteUserProfile(@RequestBody @Valid UserIdentificationDTO userIdentificationDTO);
}