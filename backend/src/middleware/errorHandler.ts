import { Request, Response, NextFunction } from 'express'

export const errorHandler = (
  err: Error,
  _req: Request,
  res: Response,
  _next: NextFunction
): void => {
  console.error(err)
  res.status(500).json({ error: 'Internal server error' })
}

export const notFound = (_req: Request, res: Response): void => {
  res.status(404).json({ error: 'Route not found' })
}
