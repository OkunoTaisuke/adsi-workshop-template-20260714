export type AttendanceStatus = "OK" | "LATE_START" | "EARLY_LEAVE" | "SHORT_HOURS";

export interface BreakResponse {
  id: number | null;
  breakStart: string | null;
  breakEnd: string | null;
}

export interface AttendanceResponse {
  id: number | null;
  date: string;
  clockIn: string | null;
  clockOut: string | null;
  breaks: BreakResponse[];
  totalWorkMinutes: number | null;
  totalBreakMinutes: number | null;
  overtimeMinutes: number | null;
  status: AttendanceStatus | null;
}

export interface AttendanceDetailResponse {
  id: number | null;
  employeeId: number;
  employeeName: string;
  departmentName: string | null;
  date: string;
  clockIn: string | null;
  clockOut: string | null;
  breaks: BreakResponse[];
  totalWorkMinutes: number | null;
  totalBreakMinutes: number | null;
  overtimeMinutes: number | null;
  status: AttendanceStatus | null;
}

export interface DepartmentResponse {
  id: number;
  name: string;
}

export interface BreakUpdateRequest {
  id?: number;
  breakStart: string;
  breakEnd: string | null;
}

export interface AttendanceUpdateRequest {
  clockIn: string | null;
  clockOut: string | null;
  breaks: BreakUpdateRequest[];
}
