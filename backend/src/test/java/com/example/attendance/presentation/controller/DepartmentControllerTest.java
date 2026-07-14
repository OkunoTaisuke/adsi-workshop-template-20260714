package com.example.attendance.presentation.controller;

import com.example.attendance.application.dto.DepartmentResponse;
import com.example.attendance.application.service.DepartmentService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DepartmentControllerTest {

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
    private DepartmentService departmentService;

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
    @DisplayName("部署一覧: ADMIN で正常取得")
    void findAll_adminRole_returns200() throws Exception {
        var token = tokenForAdmin();
        when(departmentService.findAll()).thenReturn(List.of(new DepartmentResponse(1L, "営業部")));

        mockMvc.perform(get("/departments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("営業部"));
    }

    @Test
    @DisplayName("部署一覧: USER ロールでは 403")
    void findAll_userRole_returns403() throws Exception {
        var token = tokenForUser();

        mockMvc.perform(get("/departments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("部署作成: ADMIN で正常作成")
    void create_adminRole_returns201() throws Exception {
        var token = tokenForAdmin();
        when(departmentService.create(any())).thenReturn(new DepartmentResponse(1L, "開発部"));

        mockMvc.perform(post("/departments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"開発部\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("開発部"));
    }

    @Test
    @DisplayName("部署作成: 未認証で 401")
    void create_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"開発部\"}"))
                .andExpect(status().isUnauthorized());
    }
}
