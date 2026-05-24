import { apiClient } from "./apiClient";

export interface ActivityItem {
  id: string;
  type: "attendance" | "timeoff" | "swap";
  action: string;
  detail: string;
  timestamp: string;
  userId: number;
  userName: string;
  employeeNumber: string;
  status: string | null;
  ref: Record<string, unknown>;
}

export interface ActivityFilters {
  userId?: number;
  from?: string;
  to?: string;
  type?: "all" | "attendance" | "timeoff" | "swap";
}

export async function getActivity(filters: ActivityFilters = {}): Promise<ActivityItem[]> {
  const response = await apiClient.get<ActivityItem[]>("/activity", {
    params: {
      userId: filters.userId ?? undefined,
      from:   filters.from   ?? undefined,
      to:     filters.to     ?? undefined,
      type:   filters.type   ?? undefined,
    },
  });
  return response.data;
}
