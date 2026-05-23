import { z } from 'zod'

const timeRegex = /^\d{2}:\d{2}$/
const dateRegex = /^\d{4}-\d{2}-\d{2}$/

export const createUserSchema = z.object({
  name: z.string().trim().min(1, 'name is required'),
  email: z.string().email('Invalid email format').toLowerCase(),
  employeeNumber: z.string().trim().min(1, 'employeeNumber is required'),
  role: z.enum(['ADMIN', 'MANAGER', 'EMPLOYEE'], { message: 'Invalid role' }),
  category: z.enum(['VETERINARIAN', 'NURSE', 'OPERATIONAL', 'ADMINISTRATIVE'], { message: 'Invalid category' }),
  password: z.string().min(6, 'password must be at least 6 characters')
})

export const updateUserMeSchema = z.object({
  name: z.string().trim().min(1, 'name must be a non-empty string').optional(),
  contact: z.string().trim().min(1, 'contact must be a non-empty string').optional(),
  password: z.string().min(6, 'password must be at least 6 characters').optional()
}).refine(
  data => data.name !== undefined || data.contact !== undefined || data.password !== undefined,
  { message: 'At least one field must be provided: name, contact or password' }
)

export const createShiftSchema = z.object({
  userId: z.coerce.number().int().positive('userId must be a positive integer'),
  shiftTypeId: z.number().int().positive().optional(),
  date: z.string().regex(dateRegex, 'date must be in YYYY-MM-DD format'),
  startTime: z.string().regex(timeRegex, 'startTime must be in HH:MM format').optional(),
  endTime: z.string().regex(timeRegex, 'endTime must be in HH:MM format').optional()
}).refine(
  data => data.shiftTypeId !== undefined || (data.startTime !== undefined && data.endTime !== undefined),
  { message: 'Either shiftTypeId or both startTime and endTime are required' }
)

export const updateShiftSchema = z.object({
  shiftTypeId: z.number().int().positive().optional(),
  date: z.string().regex(dateRegex, 'date must be in YYYY-MM-DD format').optional(),
  startTime: z.string().regex(timeRegex, 'startTime must be in HH:MM format').optional(),
  endTime: z.string().regex(timeRegex, 'endTime must be in HH:MM format').optional(),
  published: z.boolean().optional()
}).refine(
  data => data.shiftTypeId !== undefined || data.date !== undefined || data.published !== undefined || data.startTime !== undefined || data.endTime !== undefined,
  { message: 'At least one field must be provided' }
)

export const createShiftTypeSchema = z.object({
  name: z.string().trim().min(1, 'name is required'),
  startTime: z.string().regex(timeRegex, 'startTime must be in HH:MM format'),
  endTime: z.string().regex(timeRegex, 'endTime must be in HH:MM format')
})

export const updateShiftTypeSchema = z.object({
  name: z.string().trim().min(1, 'name must be a non-empty string').optional(),
  startTime: z.string().regex(timeRegex, 'startTime must be in HH:MM format').optional(),
  endTime: z.string().regex(timeRegex, 'endTime must be in HH:MM format').optional()
}).refine(
  data => data.name !== undefined || data.startTime !== undefined || data.endTime !== undefined,
  { message: 'At least one field must be provided' }
)

export const createTimeOffRequestSchema = z.object({
  startDate: z.string().regex(dateRegex, 'startDate must be in YYYY-MM-DD format'),
  endDate: z.string().regex(dateRegex, 'endDate must be in YYYY-MM-DD format'),
  allDay: z.boolean().default(true),
  reason: z.string().trim().optional()
}).refine(
  data => new Date(data.endDate) >= new Date(data.startDate),
  { message: 'endDate must be on or after startDate' }
)

export const updateRequestStatusSchema = z.object({
  status: z.enum(['APPROVED', 'REJECTED'], { message: 'status must be APPROVED or REJECTED' })
})

export const createShiftSwapRequestSchema = z.object({
  requesterShiftId: z.coerce.number().int().positive('requesterShiftId must be a positive integer'),
  targetShiftId: z.coerce.number().int().positive('targetShiftId must be a positive integer'),
  reason: z.string().trim().optional()
}).refine(
  data => data.requesterShiftId !== data.targetShiftId,
  { message: 'requesterShiftId and targetShiftId must be different shifts' }
)

export const createAvailabilitySchema = z.object({
  date: z.string().regex(dateRegex, 'date must be in YYYY-MM-DD format'),
  type: z.enum(['PREFERRED', 'UNAVAILABLE'], { message: 'type must be PREFERRED or UNAVAILABLE' }),
  note: z.string().trim().optional()
})

export const createAttendanceSchema = z.object({
  type: z.enum(['IN', 'OUT'], { message: 'type must be IN or OUT' }),
  note: z.string().trim().optional()
})

export const updateAttendanceSchema = z.object({
  type: z.enum(['IN', 'OUT'], { message: 'type must be IN or OUT' }).optional(),
  timestamp: z.string().datetime({ message: 'timestamp must be a valid ISO 8601 datetime' }).optional()
}).refine(
  data => data.type !== undefined || data.timestamp !== undefined,
  { message: 'At least one field must be provided: type or timestamp' }
)
