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
    const role = req.user!.role
    const { name, description, isPublic, type = 'GROUP', memberIds = [] } = req.body

    // Only ADMIN/MANAGER can create GROUP or ANNOUNCEMENT channels
    if (type !== 'DM' && role === 'EMPLOYEE') {
      return res.status(403).json({ message: 'Sem permissão para criar este tipo de canal' })
    }

    if (!name?.trim()) {
      return res.status(400).json({ message: 'Nome do canal é obrigatório' })
    }

    const derivedIsPublic = isPublic !== undefined ? isPublic : type === 'ANNOUNCEMENT'

    const channel = await prisma.channel.create({
      data: {
        name: name.trim(),
        description: description?.trim() || null,
        isPublic: derivedIsPublic,
        type: type as any,
        createdById: userId
      }
    })

    // Add creator + requested members
    const memberSet = new Set<number>([userId, ...(memberIds as number[])])
    await prisma.channelMember.createMany({
      data: Array.from(memberSet).map(uid => ({ channelId: channel.id, userId: uid })),
      skipDuplicates: true
    })

    return res.status(201).json(channel)
  } catch (err: any) {
    if (err.code === 'P2002') {
      return res.status(409).json({ message: 'Já existe um canal com esse nome' })
    }
    next(err)
  }
}

export const deleteChannel = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const userId = req.user!.userId
    const role = req.user!.role
    const channelId = parseInt(req.params.id)

    if (isNaN(channelId)) return res.status(400).json({ message: 'ID inválido' })

    const channel = await prisma.channel.findUnique({
      where: { id: channelId },
      include: { members: true }
    })
    if (!channel) return res.status(404).json({ message: 'Canal não encontrado' })

    const isMember = channel.members.some(m => m.userId === userId)

    if (channel.type === 'DM') {
      if (!isMember) return res.status(403).json({ message: 'Sem permissão' })
    } else {
      if (role !== 'ADMIN' && role !== 'MANAGER') {
        return res.status(403).json({ message: 'Sem permissão' })
      }
    }

    await prisma.channel.delete({ where: { id: channelId } })
    // ChannelMembers cascade delete; Messages get channelId = null (SetNull)
    return res.status(204).send()
  } catch (err) {
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
