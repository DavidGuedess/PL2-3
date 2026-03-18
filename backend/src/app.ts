import express from 'express'
import usersRoutes from './routes/users'


const app = express()

app.use(express.json())

app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'MiawGenda API' })
})

app.use('/users', usersRoutes)

export default app