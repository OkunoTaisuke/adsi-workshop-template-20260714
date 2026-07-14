package com.example.attendance.presentation.controller;

import com.example.attendance.application.dto.AttendanceResponse;
import com.example.attendance.application.dto.BreakResponse;
import com.example.attendance.application.service.AttendanceService;
import com.example.attendance.domain.model.AttendanceStatus;
import com.example.attendance.domain.model.Department;
import com.example.attendance.domain.model.Employee;
import com.example.attendance.domain.model.Role;
import com.example.attendance.domain.repository.EmployeeRepository;
import com.example.attendance.infrastructure.exception.AttendanceException;
import com.example.attendance.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttendanceService attendanceService;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String employeeToken;
    private String adminToken;
    private Employee employee;
    private Employee admin;

    @BeforeEach
    void setUp() {
        employee = Employee.builder()
                .id(1L)
                .email("tanaka@example.com")
                .name("田中太郎")
                .role(Role.EMPLOYEE)
                .department(Department.builder().id(1L).name("開発部").build())
                .build();

        admin = Employee.builder()
                .id(2L)
                .email("admin@example.com")
                .name("管理者太郎")
                .role(Role.ADMIN)
                .department(Department.builder().id(1L).name("開発部").build())
                .build();

        employeeToken = jwtTokenProvider.generateToken("tanaka@example.com");
        adminToken = jwtTokenProvider.generateToken("admin@example.com");

        when(employeeRepository.findByEmail("tanaka@example.com")).thenReturn(Optional.of(employee));
        when(employeeRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
    }

    @Test
    @DisplayName("POST clock-in: 正常系 200")
    void clockIn_success_returns200() throws Exception {
        var response = new AttendanceResponse(1L, LocalDate.now(), LocalTime.of(9, 0), null, List.of(), null, null, null, null);
        when(attendanceService.clockIn(any(Employee.class), any())).thenReturn(response);

        mockMvc.perform(post("/attendance/clock-in")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clockIn\": \"09:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clockIn").value("09:00:00"));
    }

    @Test
    @DisplayName("POST clock-in: 二重出勤 409")
    void clockIn_alreadyClockedIn_returns409() throws Exception {
        when(attendanceService.clockIn(any(Employee.class), any()))
                .thenThrow(AttendanceException.alreadyClockedIn());

        mockMvc.perform(post("/attendance/clock-in")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clockIn\": \"09:00\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ALREADY_CLOCKED_IN"));
    }

    @Test
    @DisplayName("POST clock-out: 正常系 200")
    void clockOut_success_returns200() throws Exception {
        var response = new AttendanceResponse(1L, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(18, 0),
                List.of(new BreakResponse(1L, LocalTime.of(12, 0), LocalTime.of(13, 0))),
                480, 60, 0, AttendanceStatus.OK);
        when(attendanceService.clockOut(any(Employee.class), any())).thenReturn(response);

        mockMvc.perform(post("/attendance/clock-out")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clockOut\": \"18:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWorkMinutes").value(480))
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    @DisplayName("POST clock-out: 未出勤 400")
    void clockOut_notClockedIn_returns400() throws Exception {
        when(attendanceService.clockOut(any(Employee.class), any()))
                .thenThrow(AttendanceException.notClockedIn());

        mockMvc.perform(post("/attendance/clock-out")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clockOut\": \"18:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("NOT_CLOCKED_IN"));
    }

    @Test
    @DisplayName("PUT 修正: 正常系 200")
    void update_success_returns200() throws Exception {
        var response = new AttendanceResponse(1L, LocalDate.now(), LocalTime.of(9, 15), LocalTime.of(18, 0),
                List.of(), 525, 0, 45, AttendanceStatus.OK);
        when(attendanceService.updateAttendance(any(Employee.class), eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/attendance/1")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clockIn\": \"09:15\", \"clockOut\": \"18:00\", \"breaks\": []}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clockIn").value("09:15:00"));
    }

    @Test
    @DisplayName("PUT 修正: 他人の記録 403")
    void update_forbidden_returns403() throws Exception {
        when(attendanceService.updateAttendance(any(Employee.class), eq(1L), any()))
                .thenThrow(AttendanceException.forbidden());

        mockMvc.perform(put("/attendance/1")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clockIn\": \"09:15\", \"clockOut\": \"18:00\", \"breaks\": []}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GET /my: 正常系")
    void getMy_success_returnsList() throws Exception {
        when(attendanceService.getMyAttendance(any(Employee.class), eq(2026), eq(7)))
                .thenReturn(List.of());

        mockMvc.perform(get("/attendance/my")
                        .header("Authorization", "Bearer " + employeeToken)
                        .param("year", "2026")
                        .param("month", "7"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /all: ADMINのみ成功")
    void getAll_admin_returns200() throws Exception {
        when(attendanceService.getAllAttendance(eq(2026), eq(7), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/attendance/all")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("year", "2026")
                        .param("month", "7"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /all: 一般社員は403")
    void getAll_employee_returns403() throws Exception {
        mockMvc.perform(get("/attendance/all")
                        .header("Authorization", "Bearer " + employeeToken)
                        .param("year", "2026")
                        .param("month", "7"))
                .andExpect(status().isForbidden());
    }
}
