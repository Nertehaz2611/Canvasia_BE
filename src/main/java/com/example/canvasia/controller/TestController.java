package com.example.canvasia.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/public/hello")
    public String publicApi() {
        return "public";
    }

    @GetMapping("/private/hello")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    public String privateApi(Authentication auth) {
        return "Hello " + auth.getName();
    }
}
