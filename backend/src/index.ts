import express from 'express'

const app = express()
const PORT = process.env.PORT || 3001

app.use(express.json())

app.get('/health', (_, res) => {
  res.json({ status: 'ok', service: 'MiawGenda API' })
})

app.listen(PORT, () => {
  console.log(`MiawGenda API running on http://localhost:${PORT}`)
})
