package com.example.attendance.application.service;

import com.example.attendance.application.dto.DepartmentRequest;
import com.example.attendance.domain.model.Department;
import com.example.attendance.domain.repository.DepartmentRepository;
import com.example.attendance.domain.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DepartmentServiceImplTest {

    private DepartmentRepository departmentRepository;
    private EmployeeRepository employeeRepository;
    private DepartmentServiceImpl service;

    @BeforeEach
    void setUp() {
        departmentRepository = mock(DepartmentRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        service = new DepartmentServiceImpl(departmentRepository, employeeRepository);
    }

    @Test
    @DisplayName("部署作成: 正常に作成できる")
    void create_validRequest_returnsDepartment() {
        var request = new DepartmentRequest("営業部");
        when(departmentRepository.existsByName("営業部")).thenReturn(false);
        when(departmentRepository.save(any())).thenAnswer(inv -> {
            Department d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });

        var result = service.create(request);

        assertThat(result.name()).isEqualTo("営業部");
        verify(departmentRepository).save(any());
    }

    @Test
    @DisplayName("部署作成: 名前重複で ConflictException")
    void create_duplicateName_throwsConflict() {
        var request = new DepartmentRequest("営業部");
        when(departmentRepository.existsByName("営業部")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("部署更新: 正常に更新できる")
    void update_validRequest_returnsDepartment() {
        var dept = Department.builder().id(1L).name("旧名").build();
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(departmentRepository.existsByName("新名")).thenReturn(false);
        when(departmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.update(1L, new DepartmentRequest("新名"));

        assertThat(result.name()).isEqualTo("新名");
    }

    @Test
    @DisplayName("部署削除: 所属社員がいる場合 ConflictException")
    void delete_hasEmployees_throwsConflict() {
        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(employeeRepository.existsByDepartmentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ConflictException.class);
        verify(departmentRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("部署削除: 所属社員なしで正常に削除できる")
    void delete_noEmployees_succeeds() {
        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(employeeRepository.existsByDepartmentId(1L)).thenReturn(false);

        service.delete(1L);

        verify(departmentRepository).deleteById(1L);
    }
}
