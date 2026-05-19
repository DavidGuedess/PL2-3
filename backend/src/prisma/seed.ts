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

  // ── Semana atual: 18-24 Mai 2026 (Seg-Dom) ─────────────────────────────────
  const seg = '2026-05-18'
  const ter = '2026-05-19'
  const qua = '2026-05-20'
  const qui = '2026-05-21'
  const sex = '2026-05-22'
  const sab = '2026-05-23'
  const dom = '2026-05-24'

  // published=true  → turno ja publicado (cor viva no ecra)
  // published=false → rascunho (cor apagada + icone cadeado no ecra)

  const shiftsToCreate = [
    // A1 (Admin / Veterinario) — Tarde Seg/Ter publicados; Manha Qui rascunho
    { userId: a1.id, shiftTypeId: tarde.id,    date: seg, published: true  },
    { userId: a1.id, shiftTypeId: tarde.id,    date: ter, published: true  },
    { userId: a1.id, shiftTypeId: manha.id,    date: qui, published: false },
    // A2 (Admin / Administrativo) — Dia Inteiro Seg publicado; Qua/Sex rascunho
    { userId: a2.id, shiftTypeId: integral.id, date: seg, published: true  },
    { userId: a2.id, shiftTypeId: integral.id, date: qua, published: false },
    { userId: a2.id, shiftTypeId: integral.id, date: sex, published: false },
    // G1 (Gerente / Veterinario) — Manha Ter publicado; Qui/Sab rascunho
    { userId: g1.id, shiftTypeId: manha.id,    date: ter, published: true  },
    { userId: g1.id, shiftTypeId: manha.id,    date: qui, published: false },
    { userId: g1.id, shiftTypeId: manha.id,    date: sab, published: false },
    // G2 (Gerente / Administrativo) — Tarde Qua/Sex rascunho; Noite Sab rascunho
    { userId: g2.id, shiftTypeId: tarde.id,    date: qua, published: false },
    { userId: g2.id, shiftTypeId: tarde.id,    date: sex, published: false },
    { userId: g2.id, shiftTypeId: noite.id,    date: sab, published: false },
    // F1 (Funcionario / Enfermeiro) — Noite Seg publicado; Ter/Dom rascunho
    { userId: f1.id, shiftTypeId: noite.id,    date: seg, published: true  },
    { userId: f1.id, shiftTypeId: noite.id,    date: ter, published: false },
    { userId: f1.id, shiftTypeId: manha.id,    date: dom, published: false },
    // F2 (Funcionario / Operacional) — Dia Inteiro Qui publicado; Sex rascunho
    { userId: f2.id, shiftTypeId: integral.id, date: qui, published: true  },
    { userId: f2.id, shiftTypeId: integral.id, date: sex, published: false },
  ]

  for (const s of shiftsToCreate) {
    await prisma.shift.upsert({
      where: { userId_date: { userId: s.userId, date: new Date(s.date) } },
      update: { shiftTypeId: s.shiftTypeId, published: s.published },
      create: {
        userId: s.userId, shiftTypeId: s.shiftTypeId,
        date: new Date(s.date), published: s.published
      }
    })
  }

  console.log('Seed concluido:')
  console.log('  6 utilizadores | 4 tipos de turno | 17 turnos (18-24 Mai 2026)')
  console.log('')
  console.log('  Login ADMIN:    EMP001 / password123  (Administrador 1 - Veterinario)')
  console.log('  Login ADMIN:    EMP002 / password123  (Administrador 2 - Administrativo)')
  console.log('  Login MANAGER:  EMP003 / password123  (Gerente 1 - Veterinario)')
  console.log('  Login MANAGER:  EMP004 / password123  (Gerente 2 - Administrativo)')
  console.log('  Login EMPLOYEE: EMP005 / password123  (Funcionario 1 - Enfermeiro)')
  console.log('  Login EMPLOYEE: EMP006 / password123  (Funcionario 2 - Operacional)')
}

main()
  .catch(e => { console.error(e); process.exit(1) })
  .finally(async () => { await prisma.$disconnect() })
