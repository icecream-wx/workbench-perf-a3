package com.example.perfa3.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EmployeeRequest(
        @NotBlank(message = "工号不能为空")
        @Size(max = 32, message = "工号长度不能超过 32")
        String employeeNo,

        @NotBlank(message = "姓名不能为空")
        @Size(max = 64, message = "姓名长度不能超过 64")
        String name,

        @Size(max = 16, message = "性别长度不能超过 16")
        @Pattern(regexp = "MALE|FEMALE|OTHER", message = "性别取值必须为 MALE、FEMALE 或 OTHER")
        String gender,

        @Size(max = 64, message = "部门长度不能超过 64")
        String department,

        @Size(max = 64, message = "职位长度不能超过 64")
        String position,

        @Size(max = 32, message = "手机号长度不能超过 32")
        String phone,

        @Size(max = 128, message = "邮箱长度不能超过 128")
        @Email(message = "邮箱格式不正确")
        String email,

        LocalDate hireDate,

        @Size(max = 16, message = "状态长度不能超过 16")
        @Pattern(regexp = "ACTIVE|INACTIVE", message = "状态取值必须为 ACTIVE 或 INACTIVE")
        String status
) {
}
