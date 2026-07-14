package com.example.attendance.application.service;

import com.example.attendance.application.dto.EmployeeCreateRequest;
import com.example.attendance.application.dto.EmployeeUpdateRequest;
import com.example.attendance.domain.model.Department;
import com.example.attendance.domain.model.Employee;
import com.example.attendance.domain.model.EmployeeRole;
import com.example.attendance.domain.model.Role;
import com.example.attendance.domain.repository.DepartmentRepository;
import com.example.attendance.domain.repository.EmployeeRepository;
import com.example.attendance.domain.repository.EmployeeRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmployeeServiceImplTest {

    private EmployeeRepository employeeRepository;
    private EmployeeRoleRepository employeeRoleRepository;
    private DepartmentRepository departmentRepository;
    private PasswordEncoder passwordEncoder;
    private EmployeeServiceImpl service;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        employeeRoleRepository = mock(EmployeeRoleRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new EmployeeServiceImpl(employeeRepository, employeeRoleRepository, departmentRepository, passwordEncoder);
    }

    @Test
    @DisplayName("社員登録: 正常に登録できる")
    void create_validRequest_returnsEmployee() {
        var request = new EmployeeCreateRequest("田中太郎", "tanaka@example.com", "password123", 1L, Set.of(Role.USER));
        when(employeeRepository.existsByEmail("tanaka@example.com")).thenReturn(false);
        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(employeeRepository.save(any())).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            e.setId(10L);
            return e;
        });
        when(employeeRoleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(Department.builder().id(1L).name("営業部").build()));

        var result = service.create(request);

        assertThat(result.name()).isEqualTo("田中太郎");
        assertThat(result.departmentName()).isEqualTo("営業部");
        assertThat(result.roles()).contains("USER");
    }

    @Test
    @DisplayName("社員登録: メール重複で ConflictException")
    void create_duplicateEmail_throwsConflict() {
        var request = new EmployeeCreateRequest("田中太郎", "tanaka@example.com", "password123", 1L, Set.of(Role.USER));
        when(employeeRepository.existsByEmail("tanaka@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("社員登録: 存在しない部署で ResourceNotFoundException")
    void create_invalidDepartment_throwsNotFound() {
        var request = new EmployeeCreateRequest("田中太郎", "tanaka@example.com", "password123", 99L, Set.of(Role.USER));
        when(employeeRepository.existsByEmail("tanaka@example.com")).thenReturn(false);
        when(departmentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("社員更新: 正常に更新できる")
    void update_validRequest_returnsUpdatedEmployee() {
        var employee = Employee.builder().id(1L).name("旧名").email("old@example.com").departmentId(1L).build();
        var request = new EmployeeUpdateRequest("新名", 2L, Set.of(Role.MANAGER));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(departmentRepository.existsById(2L)).thenReturn(true);
        when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(employeeRoleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(Department.builder().id(2L).name("開発部").build()));

        var result = service.update(1L, request);

        assertThat(result.name()).isEqualTo("新名");
        assertThat(result.departmentName()).isEqualTo("開発部");
        assertThat(result.roles()).contains("MANAGER");
        verify(employeeRoleRepository).deleteByEmployeeId(1L);
    }

    @Test
    @DisplayName("一覧取得: 全社員の詳細が返される")
    void findAll_returnsAllEmployees() {
        var emp = Employee.builder().id(1L).name("田中").email("t@e.com").departmentId(1L).build();
        when(employeeRepository.findAll()).thenReturn(List.of(emp));
        when(employeeRoleRepository.findByEmployeeIdIn(any())).thenReturn(
                List.of(EmployeeRole.builder().employeeId(1L).role(Role.USER).build()));
        when(departmentRepository.findAll()).thenReturn(
                List.of(Department.builder().id(1L).name("営業部").build()));

        var result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).departmentName()).isEqualTo("営業部");
    }
}
