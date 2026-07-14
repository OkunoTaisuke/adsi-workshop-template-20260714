import { apiFetch } from "./api-client";
import type {
  AttendanceDetailResponse,
  AttendanceResponse,
  AttendanceUpdateRequest,
  DepartmentResponse,
} from "./types";

export async function clockIn(clockIn: string): Promise<AttendanceResponse> {
  return apiFetch<AttendanceResponse>("/attendance/clock-in", {
    method: "POST",
    body: JSON.stringify({ clockIn }),
  });
}

export async function clockOut(clockOut: string): Promise<AttendanceResponse> {
  return apiFetch<AttendanceResponse>("/attendance/clock-out", {
    method: "POST",
    body: JSON.stringify({ clockOut }),
  });
}

export async function breakStart(
  breakStart: string
): Promise<AttendanceResponse> {
  return apiFetch<AttendanceResponse>("/attendance/break-start", {
    method: "POST",
    body: JSON.stringify({ breakStart }),
  });
}

export async function breakEnd(breakEnd: string): Promise<AttendanceResponse> {
  return apiFetch<AttendanceResponse>("/attendance/break-end", {
    method: "POST",
    body: JSON.stringify({ breakEnd }),
  });
}

export async function getToday(): Promise<AttendanceResponse> {
  return apiFetch<AttendanceResponse>("/attendance/today");
}

export async function getMyAttendance(
  year: number,
  month: number
): Promise<AttendanceDetailResponse[]> {
  return apiFetch<AttendanceDetailResponse[]>(
    `/attendance/my?year=${year}&month=${month}`
  );
}

export async function getAllAttendance(
  year: number,
  month: number,
  departmentId?: number,
  employeeName?: string
): Promise<AttendanceDetailResponse[]> {
  const params = new URLSearchParams({
    year: String(year),
    month: String(month),
  });
  if (departmentId) params.set("departmentId", String(departmentId));
  if (employeeName) params.set("employeeName", employeeName);
  return apiFetch<AttendanceDetailResponse[]>(
    `/attendance/all?${params.toString()}`
  );
}

export async function updateAttendance(
  id: number,
  request: AttendanceUpdateRequest
): Promise<AttendanceResponse> {
  return apiFetch<AttendanceResponse>(`/attendance/${id}`, {
    method: "PUT",
    body: JSON.stringify(request),
  });
}

export async function getDepartments(): Promise<DepartmentResponse[]> {
  return apiFetch<DepartmentResponse[]>("/departments");
}
