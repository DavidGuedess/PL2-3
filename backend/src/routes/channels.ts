import { Router } from 'express'
import { authenticate, requireRole } from '../middleware/auth'
import { getChannels, createChannel, getMessages, sendMessage } from '../controllers/channelController'

const router = Router()

router.use(authenticate)

router.get('/', getChannels)
router.post('/', requireRole('ADMIN', 'MANAGER'), createChannel)
router.get('/:id/messages', getMessages)
router.post('/:id/messages', sendMessage)

export default router
