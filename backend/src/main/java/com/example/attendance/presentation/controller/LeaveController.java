package com.example.attendance.presentation.controller;

import com.example.attendance.application.dto.LeaveCreateRequest;
import com.example.attendance.application.dto.LeaveRejectRequest;
import com.example.attendance.application.dto.LeaveResponse;
import com.example.attendance.application.service.LeaveService;
import com.example.attendance.domain.model.Employee;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping
    public ResponseEntity<LeaveResponse> create(@Valid @RequestBody LeaveCreateRequest request,
                                                @AuthenticationPrincipal Employee employee) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.create(request, employee));
    }

    @GetMapping("/my")
    public ResponseEntity<List<LeaveResponse>> myLeaves(@AuthenticationPrincipal Employee employee) {
        return ResponseEntity.ok(leaveService.findMyLeaves(employee));
    }

    @GetMapping("/subordinates")
    public ResponseEntity<List<LeaveResponse>> subordinateLeaves(@AuthenticationPrincipal Employee manager) {
        return ResponseEntity.ok(leaveService.findSubordinateLeaves(manager));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<LeaveResponse> approve(@PathVariable Long id,
                                                 @AuthenticationPrincipal Employee manager) {
        return ResponseEntity.ok(leaveService.approve(id, manager));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<LeaveResponse> reject(@PathVariable Long id,
                                                @Valid @RequestBody LeaveRejectRequest request,
                                                @AuthenticationPrincipal Employee manager) {
        return ResponseEntity.ok(leaveService.reject(id, request, manager));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<LeaveResponse> cancel(@PathVariable Long id,
                                                @AuthenticationPrincipal Employee employee) {
        return ResponseEntity.ok(leaveService.cancel(id, employee));
    }
}
