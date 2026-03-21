import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

export const createRefreshToken = async (userId: number, token: string, expiresAt: Date): Promise<void> => {
  await prisma.refreshToken.create({
    data: { userId, token, expiresAt }
  });
};

export const findRefreshToken = async (token: string) => {
  return prisma.refreshToken.findUnique({
    where: { token },
    include: { user: true }
  });
};

export const deleteRefreshToken = async (token: string): Promise<void> => {
  await prisma.refreshToken.delete({
    where: { token }
  });
};

export const deleteUserRefreshTokens = async (userId: number): Promise<void> => {
  await prisma.refreshToken.deleteMany({
    where: { userId }
  });
};
