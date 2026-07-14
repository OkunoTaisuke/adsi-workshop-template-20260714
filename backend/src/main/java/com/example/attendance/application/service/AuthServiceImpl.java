package com.example.attendance.application.service;

import com.example.attendance.application.dto.EmployeeResponse;
import com.example.attendance.application.dto.LoginRequest;
import com.example.attendance.application.dto.LoginResponse;
import com.example.attendance.domain.model.Employee;
import com.example.attendance.domain.model.EmployeeRole;
import com.example.attendance.domain.model.Role;
import com.example.attendance.domain.repository.EmployeeRepository;
import com.example.attendance.domain.repository.EmployeeRoleRepository;
import com.example.attendance.infrastructure.security.JwtTokenProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(EmployeeRepository employeeRepository,
                           EmployeeRoleRepository employeeRoleRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider) {
        this.employeeRepository = employeeRepository;
        this.employeeRoleRepository = employeeRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        var employee = employeeRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("メールアドレスまたはパスワードが正しくありません"));

        if (!passwordEncoder.matches(request.password(), employee.getPassword())) {
            throw new BadCredentialsException("メールアドレスまたはパスワードが正しくありません");
        }

        var roles = employeeRoleRepository.findByEmployeeId(employee.getId()).stream()
                .map(EmployeeRole::getRole)
                .map(Role::name)
                .toList();

        String token = jwtTokenProvider.generateToken(employee.getEmail(), roles, employee.getDepartmentId());
        var roleSet = Set.copyOf(roles);
        return new LoginResponse(token, EmployeeResponse.from(employee, roleSet));
    }

    @Override
    public EmployeeResponse getMe(Employee employee) {
        Set<String> roles = employeeRoleRepository.findByEmployeeId(employee.getId()).stream()
                .map(EmployeeRole::getRole)
                .map(Role::name)
                .collect(Collectors.toSet());
        return EmployeeResponse.from(employee, roles);
    }
}
