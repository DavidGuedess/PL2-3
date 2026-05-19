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
  shiftTypeId: z.coerce.number().int().positive('shiftTypeId must be a positive integer'),
  date: z.string().regex(dateRegex, 'date must be in YYYY-MM-DD format')
})

export const updateShiftSchema = z.object({
  shiftTypeId: z.coerce.number().int().positive().optional(),
  date: z.string().regex(dateRegex, 'date must be in YYYY-MM-DD format').optional()
}).refine(
  data => data.shiftTypeId !== undefined || data.date !== undefined,
  { message: 'shiftTypeId or date is required' }
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

export const createAttendanceSchema = z.object({
  type: z.enum(['IN', 'OUT'], { message: 'type must be IN or OUT' })
})
