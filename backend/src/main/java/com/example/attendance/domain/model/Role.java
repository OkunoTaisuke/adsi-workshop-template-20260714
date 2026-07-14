package com.example.attendance.domain.model;

public enum Role {
    USER,
    MANAGER,
    ADMIN,
    EMPLOYEE;

    public static Role normalize(Role role) {
        return role == EMPLOYEE ? USER : role;
    }
}
