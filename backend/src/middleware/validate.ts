import { ZodSchema } from 'zod'
import { Request, Response, NextFunction } from 'express'

function stripNulls(obj: any): any {
  if (typeof obj !== 'object' || obj === null || Array.isArray(obj)) return obj
  return Object.fromEntries(
    Object.entries(obj).filter(([, v]) => v !== null)
  )
}

export function validate(schema: ZodSchema) {
  return (req: Request, res: Response, next: NextFunction) => {
    const stripped = stripNulls(req.body)
    const result = schema.safeParse(stripped)
    if (!result.success) {
      const errors = result.error.errors.map(e => ({
        path: e.path.join('.'),
        message: e.message
      }))
      console.error('[validate] body received:', JSON.stringify(req.body))
      console.error('[validate] after stripNulls:', JSON.stringify(stripped))
      console.error('[validate] errors:', JSON.stringify(errors))
      return res.status(400).json({ message: 'Validation failed', errors })
    }
    req.body = result.data
    next()
  }
}
