package com.micro.learningplatform.security.controller;

import com.micro.learningplatform.security.dto.AuthenticationResponse;
import com.micro.learningplatform.security.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller // spring mvc jer vracam html view
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class OAuth2TestController {

    // http://localhost:8080/oauth2

    private static final Logger log = LogManager.getLogger(OAuth2TestController.class);
    private final AuthenticationService authenticationService;

    @GetMapping
    public String showTestPage() {
        return "oauth2-login";  // Ovo će prikazati oauth2-login.html
    }

    @GetMapping("show-login")
    public String showLoginPage(Model model,
                                @AuthenticationPrincipal OAuth2User oauth2User) {
        // Ako je korisnik već ulogiran, dodajemo njegove podatke u model
        if (oauth2User != null) {
            model.addAttribute("userEmail", oauth2User.getAttribute("email"));
            model.addAttribute("userName", oauth2User.getAttribute("name"));
            model.addAttribute("userPicture", oauth2User.getAttribute("picture"));
        }

        return "oauth2-login";  // ime HTML template-a
    }

}
