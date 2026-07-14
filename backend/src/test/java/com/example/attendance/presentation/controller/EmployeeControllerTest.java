package com.example.attendance.presentation.controller;

import com.example.attendance.application.dto.EmployeeDetailResponse;
import com.example.attendance.application.service.EmployeeService;
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
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @MockitoBean
    private EmployeeRoleRepository employeeRoleRepository;

    @MockitoBean
    private EmployeeService employeeService;

    private String tokenForAdmin() {
        var employee = Employee.builder().id(1L).email("admin@example.com")
                .password("x").name("管理者").role(Role.EMPLOYEE).departmentId(1L).build();
        when(employeeRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(employee));
        when(employeeRoleRepository.findByEmployeeId(1L))
                .thenReturn(List.of(EmployeeRole.builder().employeeId(1L).role(Role.ADMIN).build()));
        return jwtTokenProvider.generateToken("admin@example.com", List.of("ADMIN"), 1L);
    }

    private String tokenForUser() {
        var employee = Employee.builder().id(2L).email("user@example.com")
                .password("x").name("一般").role(Role.EMPLOYEE).departmentId(1L).build();
        when(employeeRepository.findByEmail("user@example.com")).thenReturn(Optional.of(employee));
        when(employeeRoleRepository.findByEmployeeId(2L))
                .thenReturn(List.of(EmployeeRole.builder().employeeId(2L).role(Role.USER).build()));
        return jwtTokenProvider.generateToken("user@example.com", List.of("USER"), 1L);
    }

    @Test
    @DisplayName("社員一覧: ADMIN で正常取得")
    void findAll_adminRole_returns200() throws Exception {
        var token = tokenForAdmin();
        when(employeeService.findAll()).thenReturn(List.of(
                new EmployeeDetailResponse(1L, "田中太郎", "tanaka@example.com", "営業部", 1L, Set.of("USER"))));

        mockMvc.perform(get("/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("田中太郎"));
    }

    @Test
    @DisplayName("社員一覧: USER ロールでは 403")
    void findAll_userRole_returns403() throws Exception {
        var token = tokenForUser();

        mockMvc.perform(get("/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("社員登録: ADMIN で正常登録")
    void create_adminRole_returns201() throws Exception {
        var token = tokenForAdmin();
        when(employeeService.create(any())).thenReturn(
                new EmployeeDetailResponse(10L, "新社員", "new@example.com", "営業部", 1L, Set.of("USER")));

        mockMvc.perform(post("/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新社員\",\"email\":\"new@example.com\",\"password\":\"password123\",\"departmentId\":1,\"roles\":[\"USER\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("新社員"));
    }

    @Test
    @DisplayName("社員登録: USER ロールでは 403")
    void create_userRole_returns403() throws Exception {
        var token = tokenForUser();

        mockMvc.perform(post("/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新社員\",\"email\":\"new@example.com\",\"password\":\"password123\",\"departmentId\":1,\"roles\":[\"USER\"]}"))
                .andExpect(status().isForbidden());
    }
}
