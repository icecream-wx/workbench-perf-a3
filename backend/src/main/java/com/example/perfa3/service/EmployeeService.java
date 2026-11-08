package com.example.perfa3.service;

import com.example.perfa3.domain.Employee;
import com.example.perfa3.dto.EmployeeRequest;
import com.example.perfa3.dto.EmployeeResponse;
import com.example.perfa3.dto.PageResponse;
import com.example.perfa3.exception.ConflictException;
import com.example.perfa3.exception.ResourceNotFoundException;
import com.example.perfa3.repository.EmployeeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_STATUS = "ACTIVE";
    private static final String NOT_FOUND_MESSAGE = "员工不存在";

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> list(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageRequest = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<Employee> result = employeeRepository.findAll(pageRequest);
        List<EmployeeResponse> items = result.getContent().stream().map(EmployeeResponse::from).toList();
        return PageResponse.of(items, result.getTotalElements(), safePage, safeSize);
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        String employeeNo = request.employeeNo().trim();
        if (employeeRepository.existsByEmployeeNo(employeeNo)) {
            throw new ConflictException("工号已存在：" + employeeNo);
        }
        Employee employee = new Employee();
        applyRequest(employee, request, employeeNo);
        employee.setCreatedAt(LocalDateTime.now());
        employee.setUpdatedAt(employee.getCreatedAt());
        try {
            Employee saved = employeeRepository.saveAndFlush(employee);
            return EmployeeResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("工号已存在：" + employeeNo);
        }
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND_MESSAGE));
        String employeeNo = request.employeeNo().trim();
        if (employeeRepository.existsByEmployeeNoAndIdNot(employeeNo, id)) {
            throw new ConflictException("工号已存在：" + employeeNo);
        }
        applyRequest(employee, request, employeeNo);
        employee.setUpdatedAt(LocalDateTime.now());
        try {
            Employee saved = employeeRepository.saveAndFlush(employee);
            return EmployeeResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("工号已存在：" + employeeNo);
        }
    }

    private void applyRequest(Employee employee, EmployeeRequest request, String employeeNo) {
        employee.setEmployeeNo(employeeNo);
        employee.setName(request.name().trim());
        employee.setGender(trimToNull(request.gender()));
        employee.setDepartment(trimToNull(request.department()));
        employee.setPosition(trimToNull(request.position()));
        employee.setPhone(trimToNull(request.phone()));
        employee.setEmail(trimToNull(request.email()));
        employee.setHireDate(request.hireDate());
        String status = trimToNull(request.status());
        employee.setStatus(status == null ? DEFAULT_STATUS : status);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
