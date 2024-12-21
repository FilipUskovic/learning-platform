package com.micro.learningplatform.security.controller;

import com.micro.learningplatform.security.dto.AuthenticationResponseWithRoles;
import com.micro.learningplatform.security.dto.RegisterRequest;
import com.micro.learningplatform.security.dto.RegisterWithRolesRequest;
import com.micro.learningplatform.security.service.AuthenticationServiceImpl;
import com.micro.learningplatform.security.UserRole;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthenticationServiceImpl authService;

    @PostMapping("/users/{email}/add-role")
    public ResponseEntity<String> addRoleToUser(@PathVariable String email, @RequestBody UserRole role) {
        authService.addRoleToUser(email, role);
        return ResponseEntity.ok("Role " + role + " added to user " + email);
    }

    @PostMapping("/users/{email}/remove-role")
    public ResponseEntity<String> removeRoleFromUser(@PathVariable String email, @RequestBody UserRole role) {
        authService.removeRoleFromUser(email, role);
        return ResponseEntity.ok("Role " + role + " removed from user " + email);
    }

    @PostMapping("/create-user")
    public ResponseEntity<AuthenticationResponseWithRoles> createUserWithRoles(@RequestBody RegisterWithRolesRequest request) {
        authService.registerUserWithRoles(request);
        return ResponseEntity.ok(authService.registerUserWithRoles(request));
    }


}
