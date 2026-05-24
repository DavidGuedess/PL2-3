import { apiClient } from "./apiClient";
import type {
  CreateShiftSwapRequestBody,
  CreateTimeOffRequestBody,
  ShiftSwapRequest,
  TimeOffRequest,
  UpdateRequestStatusBody
} from "../models/request";

export async function getTimeOffRequests(params?: { userId?: number; from?: string; to?: string }): Promise<TimeOffRequest[]> {
  const response = await apiClient.get<TimeOffRequest[]>("/time-off-requests", { params });
  return response.data;
}

export async function createTimeOffRequest(
  body: CreateTimeOffRequestBody
): Promise<TimeOffRequest> {
  const response = await apiClient.post<TimeOffRequest>("/time-off-requests", body);
  return response.data;
}

export async function updateTimeOffRequestStatus(
  id: number,
  body: UpdateRequestStatusBody
): Promise<TimeOffRequest> {
  const response = await apiClient.patch<TimeOffRequest>(
    `/time-off-requests/${id}/status`,
    body
  );

  return response.data;
}

export async function getShiftSwapRequests(params?: { userId?: number; from?: string; to?: string }): Promise<ShiftSwapRequest[]> {
  const response = await apiClient.get<ShiftSwapRequest[]>("/shift-swap-requests", { params });
  return response.data;
}

export async function createShiftSwapRequest(
  body: CreateShiftSwapRequestBody
): Promise<ShiftSwapRequest> {
  const response = await apiClient.post<ShiftSwapRequest>("/shift-swap-requests", body);
  return response.data;
}

export async function updateShiftSwapRequestStatus(
  id: number,
  body: UpdateRequestStatusBody
): Promise<ShiftSwapRequest> {
  const response = await apiClient.patch<ShiftSwapRequest>(
    `/shift-swap-requests/${id}/status`,
    body
  );

  return response.data;
}

export async function respondToSwapRequest(
  id: number,
  accept: boolean
): Promise<ShiftSwapRequest> {
  const response = await apiClient.patch<ShiftSwapRequest>(
    `/shift-swap-requests/${id}/target-response`,
    { accept }
  );

  return response.data;
}