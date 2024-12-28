package com.micro.learningplatform.security.controller;

import com.micro.learningplatform.models.User;
import com.micro.learningplatform.security.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


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



    @GetMapping("/redirect")
    public String handleRedirect(@RequestParam String access_token,
                                 @RequestParam String refresh_token,
                                 @RequestParam String token_type,
                                 Model model) {
        // Dodajemo tokene u model da ih možemo prikazati u view-u
        model.addAttribute("accessToken", access_token);
        model.addAttribute("refreshToken", refresh_token);
        model.addAttribute("tokenType", token_type);

        return "oauth2-success";  // Vratit će oauth2-success.html
    }


    @GetMapping("show-login")
    public String showLoginPage(Model model,
                                @AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User != null) {
            model.addAttribute("userEmail", oauth2User.getAttribute("email"));
            model.addAttribute("userName", oauth2User.getAttribute("name"));
            model.addAttribute("userPicture", oauth2User.getAttribute("picture"));
        } else {
            model.addAttribute("error", "Korisnik nije prijavljen.");
        }
        return "oauth2-login";
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            log.warn("Pokušaj odjave bez autentifikacije.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        authenticationService.logout(request, currentUser);

        return ResponseEntity.ok().build();
    }


}
