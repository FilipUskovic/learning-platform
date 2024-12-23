package com.micro.learningplatform.security.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/oauth2")
public class OAuth2TestController {

    @GetMapping
    public String showTestPage() {
        return "oauth2-login";  // Ovo će prikazati oauth2-login.html
    }
}
