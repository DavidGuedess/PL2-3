import 'dotenv/config'
import { PrismaClient } from '@prisma/client'
import bcrypt from 'bcryptjs'

const prisma = new PrismaClient()

async function main() {
  const passwordHash = await bcrypt.hash('password123', 10)

  // ── Utilizadores ────────────────────────────────────────────────────────────
  // A1/A2 = Administradores (ADMIN)
  // G1/G2 = Gestores (MANAGER)
  // F1/F2 = Funcionarios (EMPLOYEE)

  const a1 = await prisma.user.upsert({
    where: { employeeNumber: 'EMP001' },
    update: { name: 'Administrador 1', email: 'a1@miaugenda.com', role: 'ADMIN', category: 'VETERINARIAN', passwordHash },
    create: {
      employeeNumber: 'EMP001', name: 'Administrador 1',
      email: 'a1@miaugenda.com', passwordHash,
      role: 'ADMIN', category: 'VETERINARIAN'
    }
  })

  const a2 = await prisma.user.upsert({
    where: { employeeNumber: 'EMP002' },
    update: { name: 'Administrador 2', email: 'a2@miaugenda.com', role: 'ADMIN', category: 'ADMINISTRATIVE', passwordHash },
    create: {
      employeeNumber: 'EMP002', name: 'Administrador 2',
      email: 'a2@miaugenda.com', passwordHash,
      role: 'ADMIN', category: 'ADMINISTRATIVE'
    }
  })

  const g1 = await prisma.user.upsert({
    where: { employeeNumber: 'EMP003' },
    update: { name: 'Gerente 1', email: 'g1@miaugenda.com', role: 'MANAGER', category: 'VETERINARIAN', passwordHash },
    create: {
      employeeNumber: 'EMP003', name: 'Gerente 1',
      email: 'g1@miaugenda.com', passwordHash,
      role: 'MANAGER', category: 'VETERINARIAN'
    }
  })

  const g2 = await prisma.user.upsert({
    where: { employeeNumber: 'EMP004' },
    update: { name: 'Gerente 2', email: 'g2@miaugenda.com', role: 'MANAGER', category: 'ADMINISTRATIVE', passwordHash },
    create: {
      employeeNumber: 'EMP004', name: 'Gerente 2',
      email: 'g2@miaugenda.com', passwordHash,
      role: 'MANAGER', category: 'ADMINISTRATIVE'
    }
  })

  const f1 = await prisma.user.upsert({
    where: { employeeNumber: 'EMP005' },
    update: { name: 'Funcionario 1', email: 'f1@miaugenda.com', role: 'EMPLOYEE', category: 'NURSE', passwordHash },
    create: {
      employeeNumber: 'EMP005', name: 'Funcionario 1',
      email: 'f1@miaugenda.com', passwordHash,
      role: 'EMPLOYEE', category: 'NURSE'
    }
  })

  const f2 = await prisma.user.upsert({
    where: { employeeNumber: 'EMP006' },
    update: { name: 'Funcionario 2', email: 'f2@miaugenda.com', role: 'EMPLOYEE', category: 'OPERATIONAL', passwordHash },
    create: {
      employeeNumber: 'EMP006', name: 'Funcionario 2',
      email: 'f2@miaugenda.com', passwordHash,
      role: 'EMPLOYEE', category: 'OPERATIONAL'
    }
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

  await prisma.shift.deleteMany({
    where: { date: { gte: weekStart, lte: weekEnd } }
  })
  await prisma.attendanceRecord.deleteMany({
    where: { timestamp: { gte: weekStart, lte: weekEnd } }
  })

  // ── Semana 18-24 Mai 2026 (Seg-Dom) ─────────────────────────────────────────
  // Hoje = Sab 23. Passados: Seg-Sex. Hoje: Sab. Futuro: Dom.
  // Todos os turnos publicados (para aparecerem em "Esta Semana" no dashboard)

  const shiftsToCreate = [
    // A1 (Admin / Veterinario)
    { userId: a1.id, shiftTypeId: tarde.id,    date: '2026-05-18', published: true },
    { userId: a1.id, shiftTypeId: tarde.id,    date: '2026-05-19', published: true },
    { userId: a1.id, shiftTypeId: manha.id,    date: '2026-05-21', published: true },
    { userId: a1.id, shiftTypeId: tarde.id,    date: '2026-05-23', published: true },
    // A2 (Admin / Administrativo)
    { userId: a2.id, shiftTypeId: integral.id, date: '2026-05-18', published: true },
    { userId: a2.id, shiftTypeId: integral.id, date: '2026-05-20', published: true },
    { userId: a2.id, shiftTypeId: integral.id, date: '2026-05-22', published: true },
    // G1 (Gerente / Veterinario)
    { userId: g1.id, shiftTypeId: manha.id,    date: '2026-05-19', published: true },
    { userId: g1.id, shiftTypeId: manha.id,    date: '2026-05-21', published: true },
    { userId: g1.id, shiftTypeId: tarde.id,    date: '2026-05-23', published: true },
    // G2 (Gerente / Administrativo)
    { userId: g2.id, shiftTypeId: tarde.id,    date: '2026-05-20', published: true },
    { userId: g2.id, shiftTypeId: tarde.id,    date: '2026-05-22', published: true },
    { userId: g2.id, shiftTypeId: noite.id,    date: '2026-05-23', published: true },
    // F1 (Funcionario / Enfermeiro) — varios cenarios de ponto
    { userId: f1.id, shiftTypeId: tarde.id,    date: '2026-05-18', published: true }, // pontual
    { userId: f1.id, shiftTypeId: manha.id,    date: '2026-05-19', published: true }, // atraso
    { userId: f1.id, shiftTypeId: noite.id,    date: '2026-05-20', published: true }, // falta
    { userId: f1.id, shiftTypeId: tarde.id,    date: '2026-05-21', published: true }, // pontual
    { userId: f1.id, shiftTypeId: manha.id,    date: '2026-05-22', published: true }, // atraso leve
    { userId: f1.id, shiftTypeId: tarde.id,    date: '2026-05-23', published: true }, // hoje
    // F2 (Funcionario / Operacional) — varios cenarios de ponto
    { userId: f2.id, shiftTypeId: manha.id,    date: '2026-05-18', published: true }, // pontual
    { userId: f2.id, shiftTypeId: tarde.id,    date: '2026-05-19', published: true }, // falta
    { userId: f2.id, shiftTypeId: manha.id,    date: '2026-05-20', published: true }, // atraso
    { userId: f2.id, shiftTypeId: noite.id,    date: '2026-05-21', published: true }, // pontual
    { userId: f2.id, shiftTypeId: tarde.id,    date: '2026-05-22', published: true }, // pontual
    { userId: f2.id, shiftTypeId: manha.id,    date: '2026-05-24', published: true }, // futuro
  ]

  for (const s of shiftsToCreate) {
    await prisma.shift.create({
      data: {
        userId: s.userId, shiftTypeId: s.shiftTypeId,
        date: new Date(s.date), published: s.published
      }
    })
  }

  // ── Registos de ponto simulados ──────────────────────────────────────────────
  // Timestamps em hora local Portugal (UTC+1 em Maio 2026)
  // Formato: new Date('YYYY-MM-DDTHH:MM:SS') → interpretado como hora LOCAL pelo Node.js

  const attendance = [
    // F1 — Seg 18: Tarde 14:00-20:00, pontual
    { userId: f1.id, type: 'IN',  timestamp: new Date('2026-05-18T14:00:00') },
    { userId: f1.id, type: 'OUT', timestamp: new Date('2026-05-18T20:02:00') },
    // F1 — Ter 19: Manha 08:00-14:00, 22 min de atraso
    { userId: f1.id, type: 'IN',  timestamp: new Date('2026-05-19T08:22:00') },
    { userId: f1.id, type: 'OUT', timestamp: new Date('2026-05-19T14:05:00') },
    // F1 — Qua 20: Noite 20:00-08:00, SEM REGISTO = FALTA (sem entradas aqui)
    // F1 — Qui 21: Tarde 14:00-20:00, pontual
    { userId: f1.id, type: 'IN',  timestamp: new Date('2026-05-21T14:00:00') },
    { userId: f1.id, type: 'OUT', timestamp: new Date('2026-05-21T20:00:00') },
    // F1 — Sex 22: Manha 08:00-14:00, 9 min de atraso
    { userId: f1.id, type: 'IN',  timestamp: new Date('2026-05-22T08:09:00') },
    { userId: f1.id, type: 'OUT', timestamp: new Date('2026-05-22T14:03:00') },

    // F2 — Seg 18: Manha 08:00-14:00, pontual (2 min antes)
    { userId: f2.id, type: 'IN',  timestamp: new Date('2026-05-18T07:58:00') },
    { userId: f2.id, type: 'OUT', timestamp: new Date('2026-05-18T14:10:00') },
    // F2 — Ter 19: Tarde 14:00-20:00, SEM REGISTO = FALTA
    // F2 — Qua 20: Manha 08:00-14:00, 35 min de atraso
    { userId: f2.id, type: 'IN',  timestamp: new Date('2026-05-20T08:35:00') },
    { userId: f2.id, type: 'OUT', timestamp: new Date('2026-05-20T14:00:00') },
    // F2 — Qui 21: Noite 20:00-08:00, pontual
    { userId: f2.id, type: 'IN',  timestamp: new Date('2026-05-21T20:00:00') },
    { userId: f2.id, type: 'OUT', timestamp: new Date('2026-05-22T08:05:00') },
    // F2 — Sex 22: Tarde 14:00-20:00, pontual
    { userId: f2.id, type: 'IN',  timestamp: new Date('2026-05-22T14:03:00') },
    { userId: f2.id, type: 'OUT', timestamp: new Date('2026-05-22T20:00:00') },
  ]

  for (const a of attendance) {
    await prisma.attendanceRecord.create({ data: a })
  }

  // ── Semana seguinte: 25-31 Mai 2026 (todos publicados, futuros) ─────────────
  await prisma.shift.deleteMany({
    where: { date: { gte: new Date('2026-05-25T00:00:00.000Z'), lte: new Date('2026-05-31T23:59:59.999Z') } }
  })

  const nextWeekShifts = [
    { userId: a1.id, shiftTypeId: manha.id,    date: '2026-05-25', published: true },
    { userId: a1.id, shiftTypeId: tarde.id,    date: '2026-05-27', published: true },
    { userId: a2.id, shiftTypeId: integral.id, date: '2026-05-26', published: true },
    { userId: a2.id, shiftTypeId: tarde.id,    date: '2026-05-28', published: true },
    { userId: g1.id, shiftTypeId: manha.id,    date: '2026-05-25', published: true },
    { userId: g1.id, shiftTypeId: tarde.id,    date: '2026-05-29', published: true },
    { userId: g2.id, shiftTypeId: noite.id,    date: '2026-05-26', published: true },
    { userId: g2.id, shiftTypeId: manha.id,    date: '2026-05-30', published: true },
    { userId: f1.id, shiftTypeId: manha.id,    date: '2026-05-25', published: true },
    { userId: f1.id, shiftTypeId: tarde.id,    date: '2026-05-26', published: true },
    { userId: f1.id, shiftTypeId: noite.id,    date: '2026-05-28', published: true },
    { userId: f1.id, shiftTypeId: manha.id,    date: '2026-05-30', published: true },
    { userId: f2.id, shiftTypeId: tarde.id,    date: '2026-05-25', published: true },
    { userId: f2.id, shiftTypeId: manha.id,    date: '2026-05-27', published: true },
    { userId: f2.id, shiftTypeId: integral.id, date: '2026-05-29', published: true },
    { userId: f2.id, shiftTypeId: noite.id,    date: '2026-05-31', published: true },
  ]

  for (const s of nextWeekShifts) {
    await prisma.shift.create({
      data: { userId: s.userId, shiftTypeId: s.shiftTypeId, date: new Date(s.date), published: s.published }
    })
  }

  // ── Limpar pedidos existentes ────────────────────────────────────────────────
  await prisma.shiftSwapRequest.deleteMany({})
  await prisma.timeOffRequest.deleteMany({})

  // ── Pedidos de Férias ────────────────────────────────────────────────────────
  // F1 e F2 têm pedidos pendentes → gestor/admin vê na tab Notificações
  await prisma.timeOffRequest.create({
    data: {
      userId: f1.id,
      startDate: new Date('2026-06-02'),
      endDate:   new Date('2026-06-05'),
      allDay: true,
      reason: 'Viagem familiar',
      status: 'PENDING'
    }
  })
  await prisma.timeOffRequest.create({
    data: {
      userId: f2.id,
      startDate: new Date('2026-06-10'),
      endDate:   new Date('2026-06-11'),
      allDay: true,
      reason: 'Consulta médica',
      status: 'PENDING'
    }
  })
  // Já aprovado
  await prisma.timeOffRequest.create({
    data: {
      userId: f1.id,
      startDate: new Date('2026-05-30'),
      endDate:   new Date('2026-05-31'),
      allDay: true,
      reason: 'Fim de semana prolongado',
      status: 'APPROVED'
    }
  })
  // Já rejeitado
  await prisma.timeOffRequest.create({
    data: {
      userId: f2.id,
      startDate: new Date('2026-05-15'),
      endDate:   new Date('2026-05-16'),
      allDay: true,
      reason: 'Aniversário',
      status: 'REJECTED'
    }
  })

  // ── Pedidos de Troca ─────────────────────────────────────────────────────────
  // Buscar turnos da semana seguinte por utilizador e data
  const findShift = (userId: number, date: string) =>
    prisma.shift.findFirst({ where: { userId, date: new Date(date) } })

  const f1s25 = await findShift(f1.id, '2026-05-25') // F1 Manha 25/Mai
  const f1s26 = await findShift(f1.id, '2026-05-26') // F1 Tarde 26/Mai
  const f1s28 = await findShift(f1.id, '2026-05-28') // F1 Noite 28/Mai
  const f2s25 = await findShift(f2.id, '2026-05-25') // F2 Tarde 25/Mai
  const f2s27 = await findShift(f2.id, '2026-05-27') // F2 Manha 27/Mai
  const f2s29 = await findShift(f2.id, '2026-05-29') // F2 Integral 29/Mai

  // 1. PENDENTE — F1 pediu, F2 ainda não respondeu (target employee vê na Notificações)
  if (f1s25 && f2s25) {
    await prisma.shiftSwapRequest.create({
      data: {
        requesterId: f1.id,
        requesterShiftId: f1s25.id,
        targetShiftId: f2s25.id,
        reason: 'Preciso trocar por compromisso pessoal',
        status: 'PENDING'
      }
    })
  }

  // 2. F1 pediu troca com turno de F2 (segundo pedido pendente)
  if (f1s26 && f2s27) {
    await prisma.shiftSwapRequest.create({
      data: {
        requesterId: f1.id,
        requesterShiftId: f1s26.id,
        targetShiftId: f2s27.id,
        reason: 'Troca conveniente para ambos',
        status: 'PENDING'
      }
    })
  }

  // 3. F2 pediu troca com turno de F1 (terceiro pedido pendente)
  if (f2s29 && f1s28) {
    await prisma.shiftSwapRequest.create({
      data: {
        requesterId: f2.id,
        requesterShiftId: f2s29.id,
        targetShiftId: f1s28.id,
        reason: 'Compromisso marcado antecipadamente',
        status: 'PENDING'
      }
    })
  }

  // ── Canais padrão ───────────────────────────────────────────────────────────
  await prisma.channel.upsert({
    where: { name: 'Todos' },
    update: {},
    create: { name: 'Todos', description: 'Canal público para todos os funcionários', isPublic: true, createdById: a1.id }
  })

  await prisma.channel.upsert({
    where: { name: 'Anúncios' },
    update: {},
    create: { name: 'Anúncios', description: 'Para anúncios da gestão', isPublic: true, createdById: a1.id }
  })

  console.log('Seed concluido:')
  console.log('  6 utilizadores | 4 tipos de turno | 25+16 turnos publicados (18-31 Mai 2026)')
  console.log('  16 registos de ponto simulados (pontual / atraso / falta)')
  console.log('  4 pedidos de ferias (2 pendentes, 1 aprovado, 1 rejeitado)')
  console.log('  3 pedidos de troca (aguarda colega / aguarda gestor / aprovado)')
  console.log('')
  console.log('  Login ADMIN:    EMP001 / password123  (Administrador 1 - Veterinario)')
  console.log('  Login ADMIN:    EMP002 / password123  (Administrador 2 - Administrativo)')
  console.log('  Login MANAGER:  EMP003 / password123  (Gerente 1 - Veterinario)')
  console.log('  Login MANAGER:  EMP004 / password123  (Gerente 2 - Administrativo)')
  console.log('  Login EMPLOYEE: EMP005 / password123  (Funcionario 1 - Enfermeiro)')
  console.log('  Login EMPLOYEE: EMP006 / password123  (Funcionario 2 - Operacional)')
  console.log('')
  console.log('  F1 (EMP005) cenarios: pontual / +22min atraso / FALTA / pontual / +9min atraso / hoje')
  console.log('  F2 (EMP006) cenarios: pontual / FALTA / +35min atraso / pontual / pontual / futuro')
}

main()
  .catch(e => { console.error(e); process.exit(1) })
  .finally(async () => { await prisma.$disconnect() })
