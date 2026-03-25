import express from 'express'
import usersRoutes from './routes/users'
import authRoutes from './routes/auth'
import shiftTypesRoutes from './routes/shiftTypes'
import shiftsRoutes from './routes/shifts'


const app = express()

app.use(express.json())

app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'MiawGenda API' })
})

app.use('/users', usersRoutes)
app.use('/api/auth', authRoutes)
app.use('/shift-types', shiftTypesRoutes)
app.use('/shifts', shiftsRoutes)

export default app