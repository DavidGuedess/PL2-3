import { Request, Response } from 'express';

export const login = async (req: Request, res: Response): Promise<void> => {
  const { email, password } = req.body;

  res.status(200).json({
    message: 'Login endpoint',
    email
  });
};

export const logout = async (_req: Request, res: Response): Promise<void> => {
  res.status(200).json({ message: 'Logout successful' });
};
