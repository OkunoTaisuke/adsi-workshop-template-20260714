package com.example.attendance.application.service;

import com.example.attendance.application.dto.LoginRequest;
import com.example.attendance.domain.model.Employee;
import com.example.attendance.domain.model.EmployeeRole;
import com.example.attendance.domain.model.Role;
import com.example.attendance.domain.repository.EmployeeRepository;
import com.example.attendance.domain.repository.EmployeeRoleRepository;
import com.example.attendance.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeRoleRepository employeeRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(employeeRepository, employeeRoleRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    @DisplayName("正しい認証情報でログインするとトークンが返る")
    void login_validCredentials_returnsToken() {
        var employee = Employee.builder()
                .id(1L)
                .email("tanaka@example.com")
                .password("encoded_password")
                .name("田中太郎")
                .role(Role.EMPLOYEE)
                .departmentId(10L)
                .build();

        when(employeeRepository.findByEmail("tanaka@example.com")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(employeeRoleRepository.findByEmployeeId(1L))
                .thenReturn(List.of(EmployeeRole.builder().employeeId(1L).role(Role.USER).build()));
        when(jwtTokenProvider.generateToken(eq("tanaka@example.com"), any(), eq(10L))).thenReturn("jwt-token");

        var request = new LoginRequest("tanaka@example.com", "password123");
        var response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.employee().email()).isEqualTo("tanaka@example.com");
        assertThat(response.employee().name()).isEqualTo("田中太郎");
        assertThat(response.employee().roles()).contains("USER");
    }

    @Test
    @DisplayName("不正なパスワードでログインすると例外が発生する")
    void login_invalidPassword_throwsException() {
        var employee = Employee.builder()
                .id(1L)
                .email("tanaka@example.com")
                .password("encoded_password")
                .name("田中太郎")
                .role(Role.EMPLOYEE)
                .build();

        when(employeeRepository.findByEmail("tanaka@example.com")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        var request = new LoginRequest("tanaka@example.com", "wrong_password");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("存在しないメールアドレスでログインすると例外が発生する")
    void login_nonExistingEmail_throwsException() {
        when(employeeRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        var request = new LoginRequest("unknown@example.com", "password123");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
