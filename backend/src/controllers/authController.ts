import { Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { comparePassword } from '../utils/password';
import { generateAccessToken, generateRefreshToken, getTokenExpiration, getRefreshTokenExpiration } from '../utils/jwt';
import { addToBlacklist } from '../services/tokenBlacklist';
import { createRefreshToken, findRefreshToken, deleteRefreshToken } from '../services/refreshToken';

const prisma = new PrismaClient();

export const login = async (req: Request, res: Response): Promise<void> => {
  const { employeeNumber, password } = req.body;

  if (!employeeNumber || !password) {
    res.status(400).json({ error: 'Número de funcionário e password obrigatórios' });
    return;
  }

  const user = await prisma.user.findUnique({
    where: { employeeNumber }
  });

  if (!user || !user.active) {
    res.status(401).json({ error: 'Credenciais inválidas' });
    return;
  }

  const isValid = await comparePassword(password, user.passwordHash);

  if (!isValid) {
    res.status(401).json({ error: 'Credenciais inválidas' });
    return;
  }

  const accessToken = generateAccessToken({
    userId: user.id,
    email: user.email
  });

  const refreshToken = generateRefreshToken();
  const refreshTokenExpiration = getRefreshTokenExpiration();

  await createRefreshToken(user.id, refreshToken, refreshTokenExpiration);

  res.status(200).json({
    accessToken,
    refreshToken,
    user: {
      id: user.id,
      employeeNumber: user.employeeNumber,
      name: user.name,
      email: user.email,
      role: user.role,
      category: user.category
    }
  });
};

export const logout = async (req: Request, res: Response): Promise<void> => {
  const token = req.token!;
  const { refreshToken } = req.body;

  await addToBlacklist(token, getTokenExpiration(token));

  if (refreshToken) {
    await deleteRefreshToken(refreshToken);
  }

  res.status(200).json({ message: 'Logout successful' });
};

export const refresh = async (req: Request, res: Response): Promise<void> => {
  const { refreshToken } = req.body;

  if (!refreshToken) {
    res.status(400).json({ error: 'Refresh token obrigatório' });
    return;
  }

  const storedToken = await findRefreshToken(refreshToken);

  if (!storedToken) {
    res.status(401).json({ error: 'Refresh token inválido' });
    return;
  }

  if (storedToken.expiresAt < new Date()) {
    await deleteRefreshToken(refreshToken);
    res.status(401).json({ error: 'Refresh token expirado' });
    return;
  }

  const newAccessToken = generateAccessToken({
    userId: storedToken.user.id,
    email: storedToken.user.email
  });

  const newRefreshToken = generateRefreshToken();
  const newExpiration = getRefreshTokenExpiration();

  await deleteRefreshToken(refreshToken);
  await createRefreshToken(storedToken.user.id, newRefreshToken, newExpiration);

  res.status(200).json({
    accessToken: newAccessToken,
    refreshToken: newRefreshToken
  });
};
