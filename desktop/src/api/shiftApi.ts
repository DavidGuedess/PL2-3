import { apiClient } from "./apiClient";
import type {
  CreateShiftRequest,
  Shift,
  ShiftType,
  UpdateShiftRequest
} from "../models/shift";

export interface ShiftFilters {
  week?: string | null;
  month?: string | null;
  userId?: number | null;
}

export async function getShifts(filters: ShiftFilters = {}): Promise<Shift[]> {
  const response = await apiClient.get<Shift[]>("/shifts", {
    params: {
      week: filters.week ?? undefined,
      month: filters.month ?? undefined,
      userId: filters.userId ?? undefined
    }
  });

  return response.data;
}

export async function getMyShifts(
  filters: Omit<ShiftFilters, "userId"> = {}
): Promise<Shift[]> {
  const response = await apiClient.get<Shift[]>("/shifts/me", {
    params: {
      week: filters.week ?? undefined,
      month: filters.month ?? undefined
    }
  });

  return response.data;
}

export async function createShift(request: CreateShiftRequest): Promise<Shift> {
  const response = await apiClient.post<Shift>("/shifts", request);
  return response.data;
}

export async function updateShift(
  id: number,
  request: UpdateShiftRequest
): Promise<Shift> {
  const response = await apiClient.patch<Shift>(`/shifts/${id}`, request);
  return response.data;
}

export async function deleteShift(id: number): Promise<void> {
  await apiClient.delete(`/shifts/${id}`);
}

export async function getShiftTypes(): Promise<ShiftType[]> {
  const response = await apiClient.get<ShiftType[]>("/shift-types");
  return response.data;
}