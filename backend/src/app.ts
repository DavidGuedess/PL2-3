import express from 'express'
import usersRoutes from './routes/users'
import authRoutes from './routes/auth'
import shiftTypesRoutes from './routes/shiftTypes'
import shiftsRoutes from './routes/shifts'
import attendanceRoutes from './routes/attendance'
import weekAssignmentsRoutes from './routes/weekAssignments'
import timeOffRequestsRoutes from './routes/timeOffRequests'
import shiftSwapRequestsRoutes from './routes/shiftSwapRequests'
import availabilityRoutes from './routes/availability'
import { errorHandler } from './middleware/errorHandler'

const app = express()

app.use(express.json())

app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'MiawGenda API' })
})

app.use('/users', usersRoutes)
app.use('/api/auth', authRoutes)
app.use('/shift-types', shiftTypesRoutes)
app.use('/shifts', shiftsRoutes)
app.use('/attendance', attendanceRoutes)
app.use('/week-assignments', weekAssignmentsRoutes)
app.use('/time-off-requests', timeOffRequestsRoutes)
app.use('/shift-swap-requests', shiftSwapRequestsRoutes)
app.use('/availability', availabilityRoutes)

app.use(errorHandler)

export default app