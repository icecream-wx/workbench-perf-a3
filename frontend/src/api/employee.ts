import { get, post, put } from './http';
import type { PageResult } from './types';

export type Gender = 'MALE' | 'FEMALE' | 'OTHER';
export type EmployeeStatus = 'ACTIVE' | 'INACTIVE';

export interface Employee {
  id: number;
  employeeNo: string;
  name: string;
  gender: Gender | null;
  department: string | null;
  position: string | null;
  phone: string | null;
  email: string | null;
  hireDate: string | null;
  status: EmployeeStatus;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeInput {
  employeeNo: string;
  name: string;
  gender: Gender | null;
  department: string | null;
  position: string | null;
  phone: string | null;
  email: string | null;
  hireDate: string | null;
  status: EmployeeStatus;
}

export interface EmployeePage extends PageResult<Employee> {
  page: number;
  size: number;
}

export function listEmployees(params: { page: number; size: number }): Promise<EmployeePage> {
  return get<EmployeePage>('/employees', { params });
}

export function createEmployee(payload: EmployeeInput): Promise<Employee> {
  return post<Employee>('/employees', payload);
}

export function updateEmployee(id: number, payload: EmployeeInput): Promise<Employee> {
  return put<Employee>(`/employees/${id}`, payload);
}
