package com.example.canvasia.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/public/hello")
    public String publicApi() {
        return "public";
    }

    @GetMapping("/private/hello")
    public String privateApi(Authentication auth) {
        return "Hello " + auth.getName();
    }
}
