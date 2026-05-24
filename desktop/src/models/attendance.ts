export interface AttendanceRecord {
  id: number;
  userId: number;
  type: string;
  timestamp: string;
  note: string | null;
}

export interface CreateAttendanceBody {
  type: string;
  note?: string | null;
}

export interface ActiveEmployee {
  userId: number;
  name: string;
  employeeNumber: string;
  clockedInSince: string;
  shiftStart: string | null;
  shiftEnd: string | null;
}