package com.example.attendance.application.service;

import com.example.attendance.application.dto.EmployeeCreateRequest;
import com.example.attendance.application.dto.EmployeeDetailResponse;
import com.example.attendance.application.dto.EmployeeUpdateRequest;
import com.example.attendance.domain.model.Employee;
import com.example.attendance.domain.model.EmployeeRole;
import com.example.attendance.domain.model.Role;
import com.example.attendance.domain.repository.DepartmentRepository;
import com.example.attendance.domain.repository.EmployeeRepository;
import com.example.attendance.domain.repository.EmployeeRoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               EmployeeRoleRepository employeeRoleRepository,
                               DepartmentRepository departmentRepository,
                               PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.employeeRoleRepository = employeeRoleRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDetailResponse> findAll() {
        var employees = employeeRepository.findAll();
        var employeeIds = employees.stream().map(Employee::getId).collect(Collectors.toSet());
        var allRoles = employeeRoleRepository.findByEmployeeIdIn(employeeIds);
        var departments = departmentRepository.findAll();

        var deptMap = departments.stream()
                .collect(Collectors.toMap(d -> d.getId(), d -> d.getName()));
        var roleMap = allRoles.stream()
                .collect(Collectors.groupingBy(EmployeeRole::getEmployeeId,
                        Collectors.mapping(er -> er.getRole().name(), Collectors.toSet())));

        return employees.stream()
                .map(emp -> new EmployeeDetailResponse(
                        emp.getId(),
                        emp.getName(),
                        emp.getEmail(),
                        emp.getDepartmentId() != null ? deptMap.get(emp.getDepartmentId()) : null,
                        emp.getDepartmentId(),
                        roleMap.getOrDefault(emp.getId(), Set.of())
                ))
                .toList();
    }

    @Override
    public EmployeeDetailResponse create(EmployeeCreateRequest request) {
        if (employeeRepository.existsByEmail(request.email())) {
            throw new ConflictException("メールアドレス「" + request.email() + "」は既に登録されています");
        }
        if (!departmentRepository.existsById(request.departmentId())) {
            throw new ResourceNotFoundException("指定された部署が見つかりません");
        }

        var employee = Employee.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .departmentId(request.departmentId())
                .role(Role.EMPLOYEE)
                .build();
        var saved = employeeRepository.save(employee);

        for (Role role : request.roles()) {
            employeeRoleRepository.save(EmployeeRole.builder()
                    .employeeId(saved.getId())
                    .role(role)
                    .build());
        }

        var dept = departmentRepository.findById(request.departmentId()).orElse(null);
        var roleNames = request.roles().stream().map(Role::name).collect(Collectors.toSet());
        return new EmployeeDetailResponse(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                dept != null ? dept.getName() : null,
                saved.getDepartmentId(),
                roleNames
        );
    }

    @Override
    public EmployeeDetailResponse update(Long id, EmployeeUpdateRequest request) {
        var employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("社員が見つかりません"));
        if (!departmentRepository.existsById(request.departmentId())) {
            throw new ResourceNotFoundException("指定された部署が見つかりません");
        }

        employee.setName(request.name());
        employee.setDepartmentId(request.departmentId());
        employeeRepository.save(employee);

        employeeRoleRepository.deleteByEmployeeId(id);
        for (Role role : request.roles()) {
            employeeRoleRepository.save(EmployeeRole.builder()
                    .employeeId(id)
                    .role(role)
                    .build());
        }

        var dept = departmentRepository.findById(request.departmentId()).orElse(null);
        var roleNames = request.roles().stream().map(Role::name).collect(Collectors.toSet());
        return new EmployeeDetailResponse(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                dept != null ? dept.getName() : null,
                employee.getDepartmentId(),
                roleNames
        );
    }
}
