import { Router } from 'express'
import { authenticate } from '../middleware/auth'
import { getChannels, createChannel, deleteChannel, getMessages, sendMessage } from '../controllers/channelController'

const router = Router()

router.use(authenticate)

/**
 * @openapi
 * components:
 *   schemas:
 *     Channel:
 *       type: object
 *       properties:
 *         id:
 *           type: integer
 *         name:
 *           type: string
 *         description:
 *           type: string
 *           nullable: true
 *         isPublic:
 *           type: boolean
 *         type:
 *           type: string
 *           enum: [DM, GROUP, ANNOUNCEMENT]
 *         createdById:
 *           type: integer
 *     Message:
 *       type: object
 *       properties:
 *         id:
 *           type: integer
 *         channelId:
 *           type: integer
 *         userId:
 *           type: integer
 *         content:
 *           type: string
 *         createdAt:
 *           type: string
 *           format: date-time
 *         user:
 *           type: object
 *           properties:
 *             id:
 *               type: integer
 *             name:
 *               type: string
 */

/**
 * @openapi
 * /channels:
 *   get:
 *     summary: Listar os canais a que o utilizador tem acesso
 *     tags: [Canais]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Lista de canais (inclui a última mensagem de cada um)
 *       401:
 *         description: Não autenticado
 */
router.get('/', getChannels)

/**
 * @openapi
 * /channels:
 *   post:
 *     summary: Criar um canal (GROUP/ANNOUNCEMENT só Admin/Gestor)
 *     tags: [Canais]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required: [name]
 *             properties:
 *               name:
 *                 type: string
 *               description:
 *                 type: string
 *               isPublic:
 *                 type: boolean
 *               type:
 *                 type: string
 *                 enum: [DM, GROUP, ANNOUNCEMENT]
 *                 default: GROUP
 *               memberIds:
 *                 type: array
 *                 items:
 *                   type: integer
 *     responses:
 *       201:
 *         description: Canal criado
 *       400:
 *         description: Nome do canal em falta
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão para criar este tipo de canal
 *       409:
 *         description: Já existe um canal com esse nome
 */
router.post('/', createChannel)

/**
 * @openapi
 * /channels/{id}:
 *   delete:
 *     summary: "Eliminar um canal (DM: membro; GROUP/ANNOUNCEMENT: Admin/Gestor)"
 *     tags: [Canais]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *     responses:
 *       204:
 *         description: Canal eliminado
 *       400:
 *         description: ID inválido
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem permissão
 *       404:
 *         description: Canal não encontrado
 */
router.delete('/:id', deleteChannel)

/**
 * @openapi
 * /channels/{id}/messages:
 *   get:
 *     summary: Listar mensagens de um canal
 *     tags: [Canais]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           default: 50
 *           maximum: 100
 *       - in: query
 *         name: before
 *         schema:
 *           type: integer
 *         description: Devolve mensagens com id inferior a este (paginação)
 *     responses:
 *       200:
 *         description: Lista de mensagens por ordem cronológica
 *       400:
 *         description: ID inválido
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem acesso a este canal
 *       404:
 *         description: Canal não encontrado
 */
router.get('/:id/messages', getMessages)

/**
 * @openapi
 * /channels/{id}/messages:
 *   post:
 *     summary: Enviar uma mensagem para um canal
 *     tags: [Canais]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required: [content]
 *             properties:
 *               content:
 *                 type: string
 *     responses:
 *       201:
 *         description: Mensagem enviada
 *       400:
 *         description: ID inválido ou conteúdo em falta
 *       401:
 *         description: Não autenticado
 *       403:
 *         description: Sem acesso a este canal
 *       404:
 *         description: Canal não encontrado
 */
router.post('/:id/messages', sendMessage)

export default router
