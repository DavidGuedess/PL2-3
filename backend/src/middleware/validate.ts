import { ZodSchema } from 'zod'
import { Request, Response, NextFunction } from 'express'

export function validate(schema: ZodSchema) {
  return (req: Request, res: Response, next: NextFunction) => {
    const result = schema.safeParse(req.body)
    if (!result.success) {
      const errors = result.error.errors.map(e => ({
        path: e.path.join('.'),
        message: e.message
      }))
      return res.status(400).json({ message: 'Validation failed', errors })
    }
    req.body = result.data
    next()
  }
}
