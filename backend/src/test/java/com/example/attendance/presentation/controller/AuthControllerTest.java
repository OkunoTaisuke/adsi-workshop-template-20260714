package com.example.attendance.presentation.controller;

import com.example.attendance.application.dto.EmployeeResponse;
import com.example.attendance.application.dto.LoginRequest;
import com.example.attendance.application.dto.LoginResponse;
import com.example.attendance.application.service.AuthService;
import com.example.attendance.domain.model.Employee;
import com.example.attendance.domain.model.EmployeeRole;
import com.example.attendance.domain.model.Role;
import com.example.attendance.domain.repository.EmployeeRepository;
import com.example.attendance.domain.repository.EmployeeRoleRepository;
import com.example.attendance.infrastructure.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @MockitoBean
    private EmployeeRoleRepository employeeRoleRepository;

    @Test
    @DisplayName("正しい認証情報でログインすると200とトークンが返る")
    void login_validCredentials_returns200WithToken() throws Exception {
        var response = new LoginResponse("jwt-token",
                new EmployeeResponse(1L, "tanaka@example.com", "田中太郎", Set.of("USER"), 10L));
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("tanaka@example.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.employee.email").value("tanaka@example.com"));
    }

    @Test
    @DisplayName("不正な認証情報でログインすると401が返る")
    void login_invalidCredentials_returns401() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("メールアドレスまたはパスワードが正しくありません"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("tanaka@example.com", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("認証済みユーザーが/meにアクセスすると200とユーザー情報が返る")
    void me_authenticated_returns200WithEmployee() throws Exception {
        var employee = Employee.builder()
                .id(1L)
                .email("tanaka@example.com")
                .password("encoded")
                .name("田中太郎")
                .role(Role.EMPLOYEE)
                .departmentId(10L)
                .build();

        when(employeeRepository.findByEmail("tanaka@example.com")).thenReturn(Optional.of(employee));
        when(employeeRoleRepository.findByEmployeeId(1L))
                .thenReturn(List.of(EmployeeRole.builder().employeeId(1L).role(Role.USER).build()));
        when(authService.getMe(any(Employee.class)))
                .thenReturn(new EmployeeResponse(1L, "tanaka@example.com", "田中太郎", Set.of("USER"), 10L));
        String token = jwtTokenProvider.generateToken("tanaka@example.com", List.of("USER"), 10L);

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("tanaka@example.com"))
                .andExpect(jsonPath("$.name").value("田中太郎"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    @DisplayName("未認証で/meにアクセスすると401が返る")
    void me_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
