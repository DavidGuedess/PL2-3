import request from 'supertest'
import express from 'express'
import rateLimit from 'express-rate-limit'

function makeApp(max: number) {
  const app = express()
  app.use(express.json())

  const limiter = rateLimit({
    windowMs: 60 * 1000,
    max,
    standardHeaders: true,
    legacyHeaders: false,
    message: { error: 'Too many requests.' },
    statusCode: 429,
  })

  app.post('/test', limiter, (_req, res) => res.status(200).json({ ok: true }))
  return app
}

describe('Rate limiter middleware', () => {

  it('allows requests within the limit', async () => {
    const app = makeApp(3)

    for (let i = 0; i < 3; i++) {
      const res = await request(app).post('/test').send({})
      expect(res.status).toBe(200)
    }
  })

  it('returns 429 when limit is exceeded', async () => {
    const app = makeApp(2)

    await request(app).post('/test').send({})
    await request(app).post('/test').send({})

    const res = await request(app).post('/test').send({})
    expect(res.status).toBe(429)
    expect(res.body).toHaveProperty('error')
  })

  it('includes RateLimit headers on the response', async () => {
    const app = makeApp(5)

    const res = await request(app).post('/test').send({})
    expect(res.status).toBe(200)
    expect(res.headers).toHaveProperty('ratelimit-limit')
    expect(res.headers).toHaveProperty('ratelimit-remaining')
  })

})
