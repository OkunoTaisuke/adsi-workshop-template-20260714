package com.example.attendance.infrastructure.security;

import com.example.attendance.domain.model.Employee;
import com.example.attendance.domain.repository.EmployeeRepository;
import com.example.attendance.domain.repository.EmployeeRoleRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final EmployeeRepository employeeRepository;
    private final EmployeeRoleRepository employeeRoleRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   EmployeeRepository employeeRepository,
                                   EmployeeRoleRepository employeeRoleRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.employeeRepository = employeeRepository;
        this.employeeRoleRepository = employeeRoleRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getEmailFromToken(token);
            Employee employee = employeeRepository.findByEmail(email).orElse(null);

            if (employee != null) {
                var roles = employeeRoleRepository.findByEmployeeId(employee.getId());
                var authorities = roles.stream()
                        .map(er -> new SimpleGrantedAuthority("ROLE_" + er.getRole().name()))
                        .collect(Collectors.toList());

                var authentication = new UsernamePasswordAuthenticationToken(employee, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
