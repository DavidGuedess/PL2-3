import { Request, Response, NextFunction } from 'express'
import { PrismaClient } from '@prisma/client'

const prisma = new PrismaClient()

const messageSelect = {
  id: true,
  channelId: true,
  userId: true,
  content: true,
  createdAt: true,
  user: { select: { id: true, name: true } }
}

export const getChannels = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const userId = req.user!.userId

    const channels = await prisma.channel.findMany({
      where: {
        OR: [
          { isPublic: true },
          { members: { some: { userId } } }
        ]
      },
      include: {
        messages: {
          orderBy: { createdAt: 'desc' },
          take: 1,
          include: { user: { select: { id: true, name: true } } }
        }
      },
      orderBy: { updatedAt: 'desc' }
    })

    return res.json(channels)
  } catch (err) {
    next(err)
  }
}

export const createChannel = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const userId = req.user!.userId
    const { name, description, isPublic = true } = req.body

    if (!name?.trim()) {
      return res.status(400).json({ message: 'Nome do canal é obrigatório' })
    }

    const channel = await prisma.channel.create({
      data: {
        name: name.trim(),
        description: description?.trim() || null,
        isPublic,
        createdById: userId
      }
    })

    await prisma.channelMember.create({
      data: { channelId: channel.id, userId }
    })

    return res.status(201).json(channel)
  } catch (err: any) {
    if (err.code === 'P2002') {
      return res.status(409).json({ message: 'Já existe um canal com esse nome' })
    }
    next(err)
  }
}

export const getMessages = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const userId = req.user!.userId
    const channelId = parseInt(req.params.id)
    const limit = Math.min(parseInt(req.query.limit as string) || 50, 100)
    const before = req.query.before ? parseInt(req.query.before as string) : undefined

    if (isNaN(channelId)) return res.status(400).json({ message: 'ID inválido' })

    const channel = await prisma.channel.findUnique({ where: { id: channelId } })
    if (!channel) return res.status(404).json({ message: 'Canal não encontrado' })

    if (!channel.isPublic) {
      const member = await prisma.channelMember.findUnique({
        where: { channelId_userId: { channelId, userId } }
      })
      if (!member) return res.status(403).json({ message: 'Sem acesso a este canal' })
    }

    const where: any = { channelId }
    if (before) where.id = { lt: before }

    const messages = await prisma.message.findMany({
      where,
      orderBy: { createdAt: 'desc' },
      take: limit,
      select: messageSelect
    })

    return res.json(messages.reverse())
  } catch (err) {
    next(err)
  }
}

export const sendMessage = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const userId = req.user!.userId
    const channelId = parseInt(req.params.id)
    const { content } = req.body

    if (isNaN(channelId)) return res.status(400).json({ message: 'ID inválido' })
    if (!content?.trim()) return res.status(400).json({ message: 'Conteúdo da mensagem é obrigatório' })

    const channel = await prisma.channel.findUnique({ where: { id: channelId } })
    if (!channel) return res.status(404).json({ message: 'Canal não encontrado' })

    if (!channel.isPublic) {
      const member = await prisma.channelMember.findUnique({
        where: { channelId_userId: { channelId, userId } }
      })
      if (!member) return res.status(403).json({ message: 'Sem acesso a este canal' })
    }

    const message = await prisma.message.create({
      data: { channelId, userId, content: content.trim() },
      select: messageSelect
    })

    await prisma.channel.update({
      where: { id: channelId },
      data: { updatedAt: new Date() }
    })

    return res.status(201).json(message)
  } catch (err) {
    next(err)
  }
}
