import express from 'express'
import cors from 'cors'
import morgan from 'morgan'
import 'dotenv/config'

import authRouter from './api/routes/auth'
import usersRouter from './api/routes/users'
import shiftsRouter from './api/routes/shifts'
import { errorHandler, notFound } from './middleware/errorHandler'

const app = express()

app.use(cors({
  origin: process.env.CORS_ORIGIN ?? 'http://localhost:5173',
  credentials: true,
}))
app.use(express.json())
app.use(morgan('dev'))

app.get('/health', (_req, res) => res.json({ status: 'ok', service: 'pawschedule-api' }))

app.use('/api/auth', authRouter)
app.use('/api/users', usersRouter)
app.use('/api/shifts', shiftsRouter)

app.use(notFound)
app.use(errorHandler)

export default app
