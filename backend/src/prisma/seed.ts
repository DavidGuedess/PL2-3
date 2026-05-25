import 'dotenv/config'
import { PrismaClient } from '@prisma/client'
import bcrypt from 'bcryptjs'

const prisma = new PrismaClient()

async function main() {
  const passwordHash = await bcrypt.hash('password123', 10)

  // ── Utilizadores ────────────────────────────────────────────────────────────
  const david = await prisma.user.upsert({
    where: { employeeNumber: 'ADM001' },
    update: { name: 'David Guedes', email: 'david.guedes@miaugenda.com', role: 'ADMIN', category: 'ADMINISTRATIVE', passwordHash },
    create: { employeeNumber: 'ADM001', name: 'David Guedes', email: 'david.guedes@miaugenda.com', passwordHash, role: 'ADMIN', category: 'ADMINISTRATIVE' }
  })

  const xavier = await prisma.user.upsert({
    where: { employeeNumber: 'ADM002' },
    update: { name: 'Xavier Bolotinha', email: 'xavier.bolotinha@miaugenda.com', role: 'ADMIN', category: 'VETERINARIAN', passwordHash },
    create: { employeeNumber: 'ADM002', name: 'Xavier Bolotinha', email: 'xavier.bolotinha@miaugenda.com', passwordHash, role: 'ADMIN', category: 'VETERINARIAN' }
  })

  const leonardo = await prisma.user.upsert({
    where: { employeeNumber: 'GER001' },
    update: { name: 'Leonardo Andronési', email: 'leonardo.andronesi@miaugenda.com', role: 'MANAGER', category: 'VETERINARIAN', passwordHash },
    create: { employeeNumber: 'GER001', name: 'Leonardo Andronési', email: 'leonardo.andronesi@miaugenda.com', passwordHash, role: 'MANAGER', category: 'VETERINARIAN' }
  })

  const diogo = await prisma.user.upsert({
    where: { employeeNumber: 'GER002' },
    update: { name: 'Diogo Silva', email: 'diogo.silva@miaugenda.com', role: 'MANAGER', category: 'ADMINISTRATIVE', passwordHash },
    create: { employeeNumber: 'GER002', name: 'Diogo Silva', email: 'diogo.silva@miaugenda.com', passwordHash, role: 'MANAGER', category: 'ADMINISTRATIVE' }
  })

  // Funcionários fictícios para demonstração
  const mariana = await prisma.user.upsert({
    where: { employeeNumber: 'EMP001' },
    update: { name: 'Mariana Ferreira', email: 'mariana.ferreira@miaugenda.com', role: 'EMPLOYEE', category: 'NURSE', passwordHash },
    create: { employeeNumber: 'EMP001', name: 'Mariana Ferreira', email: 'mariana.ferreira@miaugenda.com', passwordHash, role: 'EMPLOYEE', category: 'NURSE' }
  })

  const carlos = await prisma.user.upsert({
    where: { employeeNumber: 'EMP002' },
    update: { name: 'Carlos Rodrigues', email: 'carlos.rodrigues@miaugenda.com', role: 'EMPLOYEE', category: 'OPERATIONAL', passwordHash },
    create: { employeeNumber: 'EMP002', name: 'Carlos Rodrigues', email: 'carlos.rodrigues@miaugenda.com', passwordHash, role: 'EMPLOYEE', category: 'OPERATIONAL' }
  })

  // ── Tipos de turno ──────────────────────────────────────────────────────────
  const manha = await prisma.shiftType.upsert({
    where: { name: 'Manha' },
    update: { startTime: '08:00', endTime: '14:00' },
    create: { name: 'Manha', startTime: '08:00', endTime: '14:00' }
  })

  const tarde = await prisma.shiftType.upsert({
    where: { name: 'Tarde' },
    update: { startTime: '14:00', endTime: '20:00' },
    create: { name: 'Tarde', startTime: '14:00', endTime: '20:00' }
  })

  const noite = await prisma.shiftType.upsert({
    where: { name: 'Noite' },
    update: { startTime: '20:00', endTime: '08:00' },
    create: { name: 'Noite', startTime: '20:00', endTime: '08:00' }
  })

  const integral = await prisma.shiftType.upsert({
    where: { name: 'Dia Inteiro' },
    update: { startTime: '08:00', endTime: '20:00' },
    create: { name: 'Dia Inteiro', startTime: '08:00', endTime: '20:00' }
  })

  // ── Limpar semana 18-24 Mai 2026 ────────────────────────────────────────────
  const weekStart = new Date('2026-05-18T00:00:00.000Z')
  const weekEnd   = new Date('2026-05-24T23:59:59.999Z')

  await prisma.shift.deleteMany({ where: { date: { gte: weekStart, lte: weekEnd } } })
  await prisma.attendanceRecord.deleteMany({ where: { timestamp: { gte: weekStart, lte: weekEnd } } })

  // ── Semana 18-24 Mai 2026 ────────────────────────────────────────────────────
  const shiftsToCreate = [
    { userId: david.id,    shiftTypeId: tarde.id,    date: '2026-05-18', published: true },
    { userId: david.id,    shiftTypeId: tarde.id,    date: '2026-05-19', published: true },
    { userId: david.id,    shiftTypeId: manha.id,    date: '2026-05-21', published: true },
    { userId: david.id,    shiftTypeId: tarde.id,    date: '2026-05-23', published: true },
    { userId: xavier.id,   shiftTypeId: integral.id, date: '2026-05-18', published: true },
    { userId: xavier.id,   shiftTypeId: integral.id, date: '2026-05-20', published: true },
    { userId: xavier.id,   shiftTypeId: integral.id, date: '2026-05-22', published: true },
    { userId: leonardo.id, shiftTypeId: manha.id,    date: '2026-05-19', published: true },
    { userId: leonardo.id, shiftTypeId: manha.id,    date: '2026-05-21', published: true },
    { userId: leonardo.id, shiftTypeId: tarde.id,    date: '2026-05-23', published: true },
    { userId: diogo.id,    shiftTypeId: tarde.id,    date: '2026-05-20', published: true },
    { userId: diogo.id,    shiftTypeId: tarde.id,    date: '2026-05-22', published: true },
    { userId: diogo.id,    shiftTypeId: noite.id,    date: '2026-05-23', published: true },
    { userId: mariana.id,  shiftTypeId: tarde.id,    date: '2026-05-18', published: true },
    { userId: mariana.id,  shiftTypeId: manha.id,    date: '2026-05-19', published: true },
    { userId: mariana.id,  shiftTypeId: noite.id,    date: '2026-05-20', published: true },
    { userId: mariana.id,  shiftTypeId: tarde.id,    date: '2026-05-21', published: true },
    { userId: mariana.id,  shiftTypeId: manha.id,    date: '2026-05-22', published: true },
    { userId: mariana.id,  shiftTypeId: tarde.id,    date: '2026-05-23', published: true },
    { userId: carlos.id,   shiftTypeId: manha.id,    date: '2026-05-18', published: true },
    { userId: carlos.id,   shiftTypeId: tarde.id,    date: '2026-05-19', published: true },
    { userId: carlos.id,   shiftTypeId: manha.id,    date: '2026-05-20', published: true },
    { userId: carlos.id,   shiftTypeId: noite.id,    date: '2026-05-21', published: true },
    { userId: carlos.id,   shiftTypeId: tarde.id,    date: '2026-05-22', published: true },
    { userId: carlos.id,   shiftTypeId: manha.id,    date: '2026-05-24', published: true },
  ]

  for (const s of shiftsToCreate) {
    await prisma.shift.create({
      data: { userId: s.userId, shiftTypeId: s.shiftTypeId, date: new Date(s.date), published: s.published }
    })
  }

  // ── Registos de ponto simulados ──────────────────────────────────────────────
  const attendance = [
    // Mariana — Seg 18: Tarde 14:00-20:00, pontual
    { userId: mariana.id, type: 'IN',  timestamp: new Date('2026-05-18T14:00:00') },
    { userId: mariana.id, type: 'OUT', timestamp: new Date('2026-05-18T20:02:00') },
    // Mariana — Ter 19: Manha 08:00-14:00, 22 min de atraso
    { userId: mariana.id, type: 'IN',  timestamp: new Date('2026-05-19T08:22:00') },
    { userId: mariana.id, type: 'OUT', timestamp: new Date('2026-05-19T14:05:00') },
    // Mariana — Qua 20: Noite 20:00-08:00, SEM REGISTO = FALTA
    // Mariana — Qui 21: Tarde 14:00-20:00, pontual
    { userId: mariana.id, type: 'IN',  timestamp: new Date('2026-05-21T14:00:00') },
    { userId: mariana.id, type: 'OUT', timestamp: new Date('2026-05-21T20:00:00') },
    // Mariana — Sex 22: Manha 08:00-14:00, 9 min de atraso
    { userId: mariana.id, type: 'IN',  timestamp: new Date('2026-05-22T08:09:00') },
    { userId: mariana.id, type: 'OUT', timestamp: new Date('2026-05-22T14:03:00') },

    // Carlos — Seg 18: Manha 08:00-14:00, pontual
    { userId: carlos.id, type: 'IN',  timestamp: new Date('2026-05-18T07:58:00') },
    { userId: carlos.id, type: 'OUT', timestamp: new Date('2026-05-18T14:10:00') },
    // Carlos — Ter 19: Tarde 14:00-20:00, SEM REGISTO = FALTA
    // Carlos — Qua 20: Manha 08:00-14:00, 35 min de atraso
    { userId: carlos.id, type: 'IN',  timestamp: new Date('2026-05-20T08:35:00') },
    { userId: carlos.id, type: 'OUT', timestamp: new Date('2026-05-20T14:00:00') },
    // Carlos — Qui 21: Noite 20:00-08:00, pontual
    { userId: carlos.id, type: 'IN',  timestamp: new Date('2026-05-21T20:00:00') },
    { userId: carlos.id, type: 'OUT', timestamp: new Date('2026-05-22T08:05:00') },
    // Carlos — Sex 22: Tarde 14:00-20:00, pontual
    { userId: carlos.id, type: 'IN',  timestamp: new Date('2026-05-22T14:03:00') },
    { userId: carlos.id, type: 'OUT', timestamp: new Date('2026-05-22T20:00:00') },
  ]

  for (const a of attendance) {
    await prisma.attendanceRecord.create({ data: a })
  }

  // ── Semana seguinte: 25-31 Mai 2026 ─────────────────────────────────────────
  await prisma.shift.deleteMany({
    where: { date: { gte: new Date('2026-05-25T00:00:00.000Z'), lte: new Date('2026-05-31T23:59:59.999Z') } }
  })

  const nextWeekShifts = [
    { userId: david.id,    shiftTypeId: manha.id,    date: '2026-05-25', published: true },
    { userId: david.id,    shiftTypeId: tarde.id,    date: '2026-05-27', published: true },
    { userId: xavier.id,   shiftTypeId: integral.id, date: '2026-05-26', published: true },
    { userId: xavier.id,   shiftTypeId: tarde.id,    date: '2026-05-28', published: true },
    { userId: leonardo.id, shiftTypeId: manha.id,    date: '2026-05-25', published: true },
    { userId: leonardo.id, shiftTypeId: tarde.id,    date: '2026-05-29', published: true },
    { userId: diogo.id,    shiftTypeId: noite.id,    date: '2026-05-26', published: true },
    { userId: diogo.id,    shiftTypeId: manha.id,    date: '2026-05-30', published: true },
    { userId: mariana.id,  shiftTypeId: manha.id,    date: '2026-05-25', published: true },
    { userId: mariana.id,  shiftTypeId: tarde.id,    date: '2026-05-26', published: true },
    { userId: mariana.id,  shiftTypeId: noite.id,    date: '2026-05-28', published: true },
    { userId: mariana.id,  shiftTypeId: manha.id,    date: '2026-05-30', published: true },
    { userId: carlos.id,   shiftTypeId: tarde.id,    date: '2026-05-25', published: true },
    { userId: carlos.id,   shiftTypeId: manha.id,    date: '2026-05-27', published: true },
    { userId: carlos.id,   shiftTypeId: integral.id, date: '2026-05-29', published: true },
    { userId: carlos.id,   shiftTypeId: noite.id,    date: '2026-05-31', published: true },
  ]

  for (const s of nextWeekShifts) {
    await prisma.shift.create({
      data: { userId: s.userId, shiftTypeId: s.shiftTypeId, date: new Date(s.date), published: s.published }
    })
  }

  // ── Pedidos ──────────────────────────────────────────────────────────────────
  await prisma.shiftSwapRequest.deleteMany({})
  await prisma.timeOffRequest.deleteMany({})

  await prisma.timeOffRequest.create({
    data: { userId: mariana.id, startDate: new Date('2026-06-02'), endDate: new Date('2026-06-05'), allDay: true, reason: 'Viagem familiar', status: 'PENDING' }
  })
  await prisma.timeOffRequest.create({
    data: { userId: carlos.id, startDate: new Date('2026-06-10'), endDate: new Date('2026-06-11'), allDay: true, reason: 'Consulta médica', status: 'PENDING' }
  })
  await prisma.timeOffRequest.create({
    data: { userId: mariana.id, startDate: new Date('2026-05-30'), endDate: new Date('2026-05-31'), allDay: true, reason: 'Fim de semana prolongado', status: 'APPROVED' }
  })
  await prisma.timeOffRequest.create({
    data: { userId: carlos.id, startDate: new Date('2026-05-15'), endDate: new Date('2026-05-16'), allDay: true, reason: 'Aniversário', status: 'REJECTED' }
  })

  const findShift = (userId: number, date: string) =>
    prisma.shift.findFirst({ where: { userId, date: new Date(date) } })

  const m25 = await findShift(mariana.id, '2026-05-25')
  const m26 = await findShift(mariana.id, '2026-05-26')
  const m28 = await findShift(mariana.id, '2026-05-28')
  const c25 = await findShift(carlos.id,  '2026-05-25')
  const c27 = await findShift(carlos.id,  '2026-05-27')
  const c29 = await findShift(carlos.id,  '2026-05-29')

  if (m25 && c25) await prisma.shiftSwapRequest.create({
    data: { requesterId: mariana.id, requesterShiftId: m25.id, targetShiftId: c25.id, reason: 'Compromisso pessoal', status: 'PENDING' }
  })
  if (m26 && c27) await prisma.shiftSwapRequest.create({
    data: { requesterId: mariana.id, requesterShiftId: m26.id, targetShiftId: c27.id, reason: 'Troca conveniente para ambos', status: 'PENDING' }
  })
  if (c29 && m28) await prisma.shiftSwapRequest.create({
    data: { requesterId: carlos.id, requesterShiftId: c29.id, targetShiftId: m28.id, reason: 'Compromisso marcado antecipadamente', status: 'PENDING' }
  })

  // ── Canais padrão ────────────────────────────────────────────────────────────
  await prisma.channel.upsert({
    where: { name: 'Todos' },
    update: {},
    create: { name: 'Todos', description: 'Canal público para todos os funcionários', isPublic: true, createdById: david.id }
  })
  await prisma.channel.upsert({
    where: { name: 'Anúncios' },
    update: {},
    create: { name: 'Anúncios', description: 'Para anúncios da gestão', isPublic: true, createdById: david.id }
  })

  console.log('Seed concluido:')
  console.log('  ADM001 / password123  —  Admin   — David Guedes')
  console.log('  ADM002 / password123  —  Admin   — Xavier Bolotinha')
  console.log('  GER001 / password123  —  Manager — Leonardo Andronési')
  console.log('  GER002 / password123  —  Manager — Diogo Silva')
  console.log('  EMP001 / password123  —  Employee — Mariana Ferreira')
  console.log('  EMP002 / password123  —  Employee — Carlos Rodrigues')
}

main()
  .catch(e => { console.error(e); process.exit(1) })
  .finally(async () => { await prisma.$disconnect() })
