import { Router } from 'express';
import { login, logout, refresh } from '../controllers/authController';
import { authenticate } from '../middleware/auth';
import { loginRateLimiter, refreshRateLimiter } from '../middleware/rateLimiter';

const router = Router();

/**
 * @openapi
 * /api/auth/login:
 *   post:
 *     tags: [Auth]
 *     summary: Autenticar utilizador
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required: [employeeNumber, password]
 *             properties:
 *               employeeNumber:
 *                 type: string
 *                 example: EMP001
 *               password:
 *                 type: string
 *                 format: password
 *     responses:
 *       200:
 *         description: Login bem-sucedido
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 accessToken:
 *                   type: string
 *                 refreshToken:
 *                   type: string
 *                 user:
 *                   type: object
 *                   properties:
 *                     id:
 *                       type: integer
 *                     employeeNumber:
 *                       type: string
 *                     name:
 *                       type: string
 *                     email:
 *                       type: string
 *                     role:
 *                       type: string
 *                       enum: [ADMIN, MANAGER, EMPLOYEE]
 *                     category:
 *                       type: string
 *                       enum: [VETERINARIAN, NURSE, OPERATIONAL, ADMINISTRATIVE]
 *       400:
 *         description: Campos obrigatórios em falta
 *       401:
 *         description: Credenciais inválidas
 *       429:
 *         description: Demasiadas tentativas de login
 */
router.post('/login', loginRateLimiter, login);

/**
 * @openapi
 * /api/auth/refresh:
 *   post:
 *     tags: [Auth]
 *     summary: Obter novo access token via refresh token
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required: [refreshToken]
 *             properties:
 *               refreshToken:
 *                 type: string
 *     responses:
 *       200:
 *         description: Novos tokens emitidos
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 accessToken:
 *                   type: string
 *                 refreshToken:
 *                   type: string
 *       400:
 *         description: Refresh token em falta
 *       401:
 *         description: Refresh token inválido ou expirado
 *       429:
 *         description: Demasiados pedidos de refresh token
 */
router.post('/refresh', refreshRateLimiter, refresh);

/**
 * @openapi
 * /api/auth/logout:
 *   post:
 *     tags: [Auth]
 *     summary: Terminar sessão
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               refreshToken:
 *                 type: string
 *     responses:
 *       200:
 *         description: Logout bem-sucedido
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 message:
 *                   type: string
 *                   example: Logout successful
 *       401:
 *         description: Token não fornecido ou inválido
 */
router.post('/logout', authenticate, logout);

export default router;
