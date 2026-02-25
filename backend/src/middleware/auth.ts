import { Request, Response, NextFunction } from 'express'
import { verifyToken } from '../services/jwt'

export interface AuthRequest extends Request {
  user?: { userId: number; role: string }
}

export const authenticate = (req: AuthRequest, res: Response, next: NextFunction): void => {
  const authHeader = req.headers.authorization
  if (!authHeader?.startsWith('Bearer ')) {
    res.status(401).json({ error: 'Missing or invalid token' })
    return
  }
  try {
    req.user = verifyToken(authHeader.slice(7))
    next()
  } catch {
    res.status(401).json({ error: 'Invalid or expired token' })
  }
}

export const requireRole = (...roles: string[]) =>
  (req: AuthRequest, res: Response, next: NextFunction): void => {
    if (!req.user || !roles.includes(req.user.role)) {
      res.status(403).json({ error: 'Insufficient permissions' })
      return
    }
    next()
  }

export const requireManager = requireRole('ADMIN', 'MANAGER')
export const requireAdmin = requireRole('ADMIN')
