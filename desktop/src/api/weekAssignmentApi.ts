import { apiClient } from "./apiClient";
import type {
  CreateWeekAssignmentRequest,
  WeekAssignment
} from "../models/weekAssignment";

export async function getWeekAssignments(
  week: string
): Promise<WeekAssignment[]> {
  const response = await apiClient.get<WeekAssignment[]>("/week-assignments", {
    params: {
      week
    }
  });

  return response.data;
}

export async function createWeekAssignment(
  request: CreateWeekAssignmentRequest
): Promise<WeekAssignment> {
  const response = await apiClient.post<WeekAssignment>(
    "/week-assignments",
    request
  );

  return response.data;
}

export async function deleteWeekAssignment(id: number): Promise<void> {
  await apiClient.delete(`/week-assignments/${id}`);
}