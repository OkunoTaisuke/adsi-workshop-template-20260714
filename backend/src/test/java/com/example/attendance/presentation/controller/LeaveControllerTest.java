package com.example.attendance.presentation.controller;

import com.example.attendance.application.dto.LeaveCreateRequest;
import com.example.attendance.application.dto.LeaveResponse;
import com.example.attendance.application.service.LeaveService;
import com.example.attendance.domain.model.Employee;
import com.example.attendance.domain.model.EmployeeRole;
import com.example.attendance.domain.model.LeavePeriodType;
import com.example.attendance.domain.model.LeaveType;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeaveControllerTest {

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
    private LeaveService leaveService;

    private String tokenForUser() {
        var employee = Employee.builder().id(1L).email("tanaka@example.com")
                .password("x").name("田中太郎").role(Role.EMPLOYEE).departmentId(10L).build();
        when(employeeRepository.findByEmail("tanaka@example.com")).thenReturn(Optional.of(employee));
        when(employeeRoleRepository.findByEmployeeId(1L))
                .thenReturn(List.of(EmployeeRole.builder().employeeId(1L).role(Role.USER).build()));
        return jwtTokenProvider.generateToken("tanaka@example.com", List.of("USER"), 10L);
    }

    private String tokenForManager() {
        var employee = Employee.builder().id(2L).email("sato@example.com")
                .password("x").name("佐藤花子").role(Role.EMPLOYEE).departmentId(10L).build();
        when(employeeRepository.findByEmail("sato@example.com")).thenReturn(Optional.of(employee));
        when(employeeRoleRepository.findByEmployeeId(2L))
                .thenReturn(List.of(EmployeeRole.builder().employeeId(2L).role(Role.MANAGER).build()));
        return jwtTokenProvider.generateToken("sato@example.com", List.of("MANAGER"), 10L);
    }

    @Test
    @DisplayName("休暇申請: 正常に作成できる")
    void create_validRequest_returns201() throws Exception {
        var token = tokenForUser();
        var response = new LeaveResponse(1L, 1L, "田中太郎", "PAID", "FULL_DAY", "全日",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1), "私用",
                "PENDING", "申請中", null, null, null, LocalDateTime.now());
        when(leaveService.create(any(), any())).thenReturn(response);

        var request = new LeaveCreateRequest(LeaveType.PAID, LeavePeriodType.FULL_DAY,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1), "私用");

        mockMvc.perform(post("/leaves")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("自分の休暇一覧: 正常に取得できる")
    void myLeaves_returns200() throws Exception {
        var token = tokenForUser();
        when(leaveService.findMyLeaves(any())).thenReturn(List.of());

        mockMvc.perform(get("/leaves/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("部下の申請一覧: MANAGER でないと 403")
    void subordinateLeaves_userRole_returns403() throws Exception {
        var token = tokenForUser();

        mockMvc.perform(get("/leaves/subordinates")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("部下の申請一覧: MANAGER で正常取得")
    void subordinateLeaves_managerRole_returns200() throws Exception {
        var token = tokenForManager();
        when(leaveService.findSubordinateLeaves(any())).thenReturn(List.of());

        mockMvc.perform(get("/leaves/subordinates")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("承認: MANAGER で正常に承認できる")
    void approve_managerRole_returns200() throws Exception {
        var token = tokenForManager();
        var response = new LeaveResponse(1L, 1L, "田中太郎", "PAID", "FULL_DAY", "全日",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1), null,
                "APPROVED", "承認", "佐藤花子", LocalDateTime.now(), null, LocalDateTime.now());
        when(leaveService.approve(any(), any())).thenReturn(response);

        mockMvc.perform(put("/leaves/1/approve")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("承認: USER ロールでは 403")
    void approve_userRole_returns403() throws Exception {
        var token = tokenForUser();

        mockMvc.perform(put("/leaves/1/approve")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
