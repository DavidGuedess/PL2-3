import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

export const addToBlacklist = async (token: string, expiresAt: Date): Promise<void> => {
  await prisma.tokenBlacklist.create({
    data: { token, expiresAt }
  });
};

export const isTokenBlacklisted = async (token: string): Promise<boolean> => {
  const blacklisted = await prisma.tokenBlacklist.findUnique({
    where: { token }
  });
  return !!blacklisted;
};
