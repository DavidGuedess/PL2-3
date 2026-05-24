import type { Shift } from "./shift";
import type { User } from "./user";

export interface TimeOffRequest {
  id: number;
  userId: number;
  startDate: string;
  endDate: string;
  reason: string | null;
  status: string;
  allDay: boolean;
  createdAt: string;
  updatedAt: string;
  approvedByName?: string | null;
  user?: User | null;
}

export interface ShiftSwapRequest {
  id: number;
  requesterId: number;
  requesterShiftId: number;
  targetShiftId: number;
  reason: string | null;
  status: string;
  targetAccepted: boolean | null;
  createdAt: string;
  updatedAt: string;
  approvedByName?: string | null;
  requester?: User | null;
  requesterShift?: Shift | null;
  targetShift?: Shift | null;
}

export interface CreateTimeOffRequestBody {
  startDate: string;
  endDate: string;
  reason?: string | null;
  allDay?: boolean;
}

export interface CreateShiftSwapRequestBody {
  requesterShiftId: number;
  targetShiftId: number;
  reason?: string | null;
}

export interface UpdateRequestStatusBody {
  status: string;
}