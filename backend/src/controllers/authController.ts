import { Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { comparePassword } from '../utils/password';
import { generateToken, getTokenExpiration } from '../utils/jwt';
import { addToBlacklist } from '../services/tokenBlacklist';

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

  const token = generateToken({
    userId: user.id,
    email: user.email
  });

  res.status(200).json({
    token,
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
  const token = req.token;

  if (!token) {
    res.status(400).json({ error: 'Token não encontrado' });
    return;
  }

  const expiresAt = getTokenExpiration(token);
  await addToBlacklist(token, expiresAt);

  res.status(200).json({ message: 'Logout efetuado com sucesso' });
};
