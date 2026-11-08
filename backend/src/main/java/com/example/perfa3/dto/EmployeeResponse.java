package com.example.perfa3.dto;

import com.example.perfa3.domain.Employee;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeResponse(
        Long id,
        String employeeNo,
        String name,
        String gender,
        String department,
        String position,
        String phone,
        String email,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate hireDate,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt
) {

    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeNo(),
                employee.getName(),
                employee.getGender(),
                employee.getDepartment(),
                employee.getPosition(),
                employee.getPhone(),
                employee.getEmail(),
                employee.getHireDate(),
                employee.getStatus(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}
