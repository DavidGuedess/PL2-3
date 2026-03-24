import express from 'express'
import usersRoutes from './routes/users'
import authRoutes from './routes/auth'


const app = express()

app.use(express.json())

app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'MiawGenda API' })
})

app.use('/users', usersRoutes)
app.use('/api/auth', authRoutes)

export default app