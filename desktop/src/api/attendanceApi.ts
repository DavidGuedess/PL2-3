import { apiClient } from "./apiClient";
import type {
  ActiveEmployee,
  AttendanceRecord,
  CreateAttendanceBody
} from "../models/attendance";

export interface AttendanceFilters {
  userId?: number | null;
  from?: string | null;
  to?: string | null;
}

export async function registerAttendance(
  body: CreateAttendanceBody
): Promise<AttendanceRecord> {
  const response = await apiClient.post<AttendanceRecord>("/attendance", body);
  return response.data;
}

export async function getMyAttendanceHistory(
  filters: Omit<AttendanceFilters, "userId"> = {}
): Promise<AttendanceRecord[]> {
  const response = await apiClient.get<AttendanceRecord[]>("/attendance/me", {
    params: {
      from: filters.from ?? undefined,
      to: filters.to ?? undefined
    }
  });

  return response.data;
}

export async function getAttendance(
  filters: AttendanceFilters = {}
): Promise<AttendanceRecord[]> {
  const response = await apiClient.get<AttendanceRecord[]>("/attendance", {
    params: {
      userId: filters.userId ?? undefined,
      from: filters.from ?? undefined,
      to: filters.to ?? undefined
    }
  });

  return response.data;
}

export async function getActiveEmployees(): Promise<ActiveEmployee[]> {
  const response = await apiClient.get<ActiveEmployee[]>("/attendance/active");
  return response.data;
}