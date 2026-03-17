import { Router } from 'express'
import { logout, login } from '../controllers/authController'

const router = Router()

router.post('/logout', logout)
router.post('/login', login);

export default router;