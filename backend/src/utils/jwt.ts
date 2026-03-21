import jwt from 'jsonwebtoken'
import crypto from 'crypto'

const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-key'
const ACCESS_TOKEN_EXPIRES_IN = '15m'
const REFRESH_TOKEN_EXPIRES_IN = '30d'

export interface TokenPayload {
  userId: number
  email: string
}

export const generateAccessToken = (payload: TokenPayload): string => {
  return jwt.sign(payload, JWT_SECRET, { expiresIn: ACCESS_TOKEN_EXPIRES_IN })
}

export const generateRefreshToken = (): string => {
  return crypto.randomBytes(64).toString('hex')
}

export const verifyToken = (token: string): TokenPayload => {
  return jwt.verify(token, JWT_SECRET) as TokenPayload
}

export const getTokenExpiration = (token: string): Date => {
  const decoded = jwt.decode(token) as { exp: number }
  return new Date(decoded.exp * 1000)
}

export const getRefreshTokenExpiration = (): Date => {
  return new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
}
