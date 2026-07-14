package com.example.attendance.presentation.controller;

import com.example.attendance.application.dto.EmployeeResponse;
import com.example.attendance.application.dto.LoginRequest;
import com.example.attendance.application.dto.LoginResponse;
import com.example.attendance.application.service.AuthService;
import com.example.attendance.domain.model.Employee;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<EmployeeResponse> me(@AuthenticationPrincipal Employee employee) {
        return ResponseEntity.ok(authService.getMe(employee));
    }
}
