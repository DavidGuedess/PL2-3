import { Request, Response, NextFunction } from 'express'
import { verifyToken, TokenPayload } from '../utils/jwt'
import { isTokenBlacklisted } from '../services/tokenBlacklist'

declare global {
  namespace Express {
    interface Request {
      user?: TokenPayload
      token?: string
    }
  }
}

export const authenticate = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
  const authHeader = req.headers.authorization

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    res.status(401).json({ error: 'Token não fornecido' })
    return
  }

  const token = authHeader.substring(7)

  try {
    const blacklisted = await isTokenBlacklisted(token)

    if (blacklisted) {
      res.status(401).json({ error: 'Token inválido' })
      return
    }

    const payload = verifyToken(token)
    req.user = payload
    req.token = token
    next()
  } catch (error) {
    res.status(401).json({ error: 'Token inválido' })
  }
}
