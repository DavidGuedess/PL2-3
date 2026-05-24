/* import express from 'express'
import { runAutoClockOut } from './controllers/attendanceController'
import usersRoutes from './routes/users'
import authRoutes from './routes/auth'
import shiftTypesRoutes from './routes/shiftTypes'
import shiftsRoutes from './routes/shifts'
import attendanceRoutes from './routes/attendance'
import weekAssignmentsRoutes from './routes/weekAssignments'
import channelsRoutes from './routes/channels'
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
app.use('/channels', channelsRoutes)
app.use('/time-off-requests', timeOffRequestsRoutes)
app.use('/shift-swap-requests', shiftSwapRequestsRoutes)
app.use('/availability', availabilityRoutes)

app.use(errorHandler)

// Verifica saídas automáticas de minuto a minuto
setInterval(runAutoClockOut, 60_000)

export default app */


import express from 'express'
import cors from 'cors'
import { runAutoClockOut } from './controllers/attendanceController'
import usersRoutes from './routes/users'
import authRoutes from './routes/auth'
import shiftTypesRoutes from './routes/shiftTypes'
import shiftsRoutes from './routes/shifts'
import attendanceRoutes from './routes/attendance'
import weekAssignmentsRoutes from './routes/weekAssignments'
import channelsRoutes from './routes/channels'
import timeOffRequestsRoutes from './routes/timeOffRequests'
import shiftSwapRequestsRoutes from './routes/shiftSwapRequests'
import availabilityRoutes from './routes/availability'
import { errorHandler } from './middleware/errorHandler'

const app = express()

app.use(cors({
  origin: [
    'http://localhost:5173',
    'http://localhost:5174',
    'http://127.0.0.1:5173',
    'http://127.0.0.1:5174',
    'http://192.168.0.30:5173'
  ],
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
  credentials: true
}))

app.options('*', cors())

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
app.use('/channels', channelsRoutes)
app.use('/time-off-requests', timeOffRequestsRoutes)
app.use('/shift-swap-requests', shiftSwapRequestsRoutes)
app.use('/availability', availabilityRoutes)
app.use('/activity', activityRoutes)

app.use(errorHandler)

setInterval(runAutoClockOut, 60_000)

export default app