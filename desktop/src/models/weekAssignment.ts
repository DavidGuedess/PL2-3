import type { User } from "./user";

export interface WeekAssignment {
  id: number;
  userId: number;
  weekStart: string;
  user: User;
}

export interface CreateWeekAssignmentRequest {
  userId: number;
  weekStart: string;
}