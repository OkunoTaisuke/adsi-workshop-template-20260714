package com.example.attendance.application.service;

import com.example.attendance.application.dto.LoginRequest;
import com.example.attendance.application.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
