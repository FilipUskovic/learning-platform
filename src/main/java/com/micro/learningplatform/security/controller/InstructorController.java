package com.micro.learningplatform.security.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/instructor")
@RequiredArgsConstructor
public class InstructorController {

    @GetMapping("/dashboard")
    public ResponseEntity<String> getInstructorDashboard() {
        return ResponseEntity.ok("Welcome to Instructor Dashboard!");
    }
}
