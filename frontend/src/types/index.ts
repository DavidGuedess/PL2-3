export type Role = 'ADMIN' | 'MANAGER' | 'EMPLOYEE'
export type ShiftType = 'MORNING' | 'AFTERNOON' | 'NIGHT'
export type RequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type RequestType = 'SWAP' | 'DAY_OFF' | 'VACATION'

export interface User {
  id: number
  employeeNumber: string
  fullName: string
  email: string
  role: Role
  department?: string
  isActive: boolean
  createdAt: string
}

export interface ShiftTemplate {
  id: number
  name: string
  shiftType: ShiftType
  startTime: string
  endTime: string
  description?: string
  isActive: boolean
}

export interface ShiftAssignment {
  id: number
  date: string
  notes?: string
  employeeId: number
  employee: Pick<User, 'id' | 'fullName' | 'employeeNumber'>
  shiftTemplate: ShiftTemplate
}

export interface ShiftRequest {
  id: number
  requestType: RequestType
  status: RequestStatus
  startDate?: string
  endDate?: string
  justification?: string
  managerComment?: string
  reviewedAt?: string
  createdAt: string
  requesterId: number
  requester: Pick<User, 'id' | 'fullName' | 'employeeNumber'>
  targetEmployee?: Pick<User, 'id' | 'fullName' | 'employeeNumber'>
  originalAssignment?: ShiftAssignment
}
