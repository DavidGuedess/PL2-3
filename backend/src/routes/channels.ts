import { Router } from 'express'
import { authenticate } from '../middleware/auth'
import { getChannels, createChannel, deleteChannel, getMessages, sendMessage } from '../controllers/channelController'

const router = Router()

router.use(authenticate)

router.get('/', getChannels)
router.post('/', createChannel)
router.delete('/:id', deleteChannel)
router.get('/:id/messages', getMessages)
router.post('/:id/messages', sendMessage)

export default router
