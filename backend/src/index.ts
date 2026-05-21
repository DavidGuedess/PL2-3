import express from 'express'
import swaggerUi from 'swagger-ui-express'
import usersRoutes from './routes/users'
import authRoutes from './routes/auth'
import { swaggerSpec } from './swagger'
import app from './app'

const PORT = process.env.PORT || 3001

app.use(express.json())
app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerSpec))
app.use((req, _res, next) => {
  console.log('req.body:', req.body)
  next()
})

/**
 * @openapi
 * /health:
 *   get:
 *     tags: [System]
 *     summary: Verificar estado da API
 *     responses:
 *       200:
 *         description: API operacional
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 status:
 *                   type: string
 *                   example: ok
 *                 service:
 *                   type: string
 *                   example: Miaugenda API
 */
app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'Miaugenda API' })
})

app.use('/users', usersRoutes)
app.use('/api/auth', authRoutes)

app.listen(Number(PORT), '0.0.0.0', () => {
  console.log(`Miaugenda API running on http://0.0.0.0:${PORT}`)
})
