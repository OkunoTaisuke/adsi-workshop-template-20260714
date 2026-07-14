package com.example.attendance.application.service;

import com.example.attendance.application.dto.DepartmentRequest;
import com.example.attendance.application.dto.DepartmentResponse;
import com.example.attendance.domain.model.Department;
import com.example.attendance.domain.repository.DepartmentRepository;
import com.example.attendance.domain.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository,
                                 EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> findAll() {
        return departmentRepository.findAll().stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    @Override
    public DepartmentResponse create(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.name())) {
            throw new ConflictException("部署名「" + request.name() + "」は既に存在します");
        }
        var department = Department.builder()
                .name(request.name())
                .build();
        return DepartmentResponse.from(departmentRepository.save(department));
    }

    @Override
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        var department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("部署が見つかりません"));
        if (!department.getName().equals(request.name()) && departmentRepository.existsByName(request.name())) {
            throw new ConflictException("部署名「" + request.name() + "」は既に存在します");
        }
        department.setName(request.name());
        return DepartmentResponse.from(departmentRepository.save(department));
    }

    @Override
    public void delete(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("部署が見つかりません");
        }
        if (employeeRepository.existsByDepartmentId(id)) {
            throw new ConflictException("所属社員が存在するため削除できません");
        }
        departmentRepository.deleteById(id);
    }
}
