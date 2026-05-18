import { Request, Response, NextFunction } from 'express'

export function errorHandler(err: unknown, _req: Request, res: Response, _next: NextFunction): void {
  const code = (err as any)?.code

  if (code === 'P2002') {
    const targets: string[] = (err as any).meta?.target ?? []
    if (targets.includes('email')) {
      res.status(409).json({ message: 'Email already exists' })
      return
    }
    if (targets.includes('employeeNumber')) {
      res.status(409).json({ message: 'Employee number already exists' })
      return
    }
    res.status(409).json({ message: 'Recurso já existe' })
    return
  }

  if (code === 'P2025') {
    res.status(404).json({ message: 'Recurso não encontrado' })
    return
  }

  console.error(err)
  res.status(500).json({ message: 'Erro interno do servidor' })
}
