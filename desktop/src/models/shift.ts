export interface ShiftUser {
  id: number;
  name: string;
  employeeNumber: string;
  role: string;
  category: string;
}

export interface ShiftType {
  id: number;
  name: string;
  startTime: string;
  endTime: string;
}

export interface Shift {
  id: number;
  userId: number;
  shiftTypeId: number | null;
  date: string;
  startTime: string | null;
  endTime: string | null;
  published: boolean;
  user: ShiftUser | null;
  shiftType: ShiftType | null;
}

export interface CreateShiftRequest {
  userId: number;
  date: string;
  startTime: string;
  endTime: string;
  shiftTypeId?: number | null;
}

export interface UpdateShiftRequest {
  userId?: number;
  date?: string;
  startTime?: string;
  endTime?: string;
  shiftTypeId?: number | null;
  published?: boolean;
}

export function resolvedStartTime(shift: Shift): string {
  return shift.startTime ?? shift.shiftType?.startTime ?? "00:00";
}

export function resolvedEndTime(shift: Shift): string {
  return shift.endTime ?? shift.shiftType?.endTime ?? "00:00";
}

export function resolvedName(shift: Shift): string {
  return shift.shiftType?.name ?? `${resolvedStartTime(shift)}-${resolvedEndTime(shift)}`;
}