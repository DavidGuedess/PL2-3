import app from './app'

const PORT = process.env.PORT ?? 3001

app.listen(PORT, () => {
  console.log(`PawSchedule API running on http://localhost:${PORT}`)
  console.log(`Environment: ${process.env.NODE_ENV ?? 'development'}`)
})
