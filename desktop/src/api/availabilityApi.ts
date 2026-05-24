import { apiClient } from "./apiClient";
import type { Availability, CreateAvailabilityBody } from "../models/availability";

export interface AvailabilityFilters {
  startDate?: string | null;
  endDate?: string | null;
  userId?: number | null;
}

export async function getAvailabilities(
  filters: AvailabilityFilters = {}
): Promise<Availability[]> {
  const response = await apiClient.get<Availability[]>("/availability", {
    params: {
      startDate: filters.startDate ?? undefined,
      endDate: filters.endDate ?? undefined,
      userId: filters.userId ?? undefined
    }
  });

  return response.data;
}

export async function createAvailability(
  body: CreateAvailabilityBody
): Promise<Availability> {
  const response = await apiClient.post<Availability>("/availability", body);
  return response.data;
}

export async function deleteAvailability(id: number): Promise<void> {
  await apiClient.delete(`/availability/${id}`);
}