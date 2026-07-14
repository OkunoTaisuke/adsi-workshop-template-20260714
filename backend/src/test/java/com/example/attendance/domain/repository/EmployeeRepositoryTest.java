package com.example.attendance.domain.repository;

import com.example.attendance.domain.model.Employee;
import com.example.attendance.domain.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    @DisplayName("社員を保存すると正しく永続化される")
    void save_validEmployee_persistsSuccessfully() {
        var employee = Employee.builder()
                .email("new-user@example.com")
                .password("hashed_password")
                .name("新規ユーザー")
                .role(Role.EMPLOYEE)
                .build();

        var saved = employeeRepository.save(employee);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("new-user@example.com");
        assertThat(saved.getName()).isEqualTo("新規ユーザー");
        assertThat(saved.getRole()).isEqualTo(Role.EMPLOYEE);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("存在するメールアドレスで検索すると社員が返る")
    void findByEmail_existingEmail_returnsEmployee() {
        var employee = Employee.builder()
                .email("test-find@example.com")
                .password("hashed_password")
                .name("検索テスト")
                .role(Role.ADMIN)
                .build();
        employeeRepository.save(employee);

        Optional<Employee> found = employeeRepository.findByEmail("test-find@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("検索テスト");
        assertThat(found.get().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("存在しないメールアドレスで検索すると空が返る")
    void findByEmail_nonExistingEmail_returnsEmpty() {
        Optional<Employee> found = employeeRepository.findByEmail("notfound@example.com");

        assertThat(found).isEmpty();
    }
}
