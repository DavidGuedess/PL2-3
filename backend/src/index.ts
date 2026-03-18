import express from 'express'
import usersRoutes from './routes/users'

const app = express()
const PORT = process.env.PORT || 3001

app.use(express.json())

app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'MiawGenda API' })
})

app.use('/users', usersRoutes)

app.listen(PORT, () => {
  console.log(`MiawGenda API running on http://localhost:${PORT}`)
})