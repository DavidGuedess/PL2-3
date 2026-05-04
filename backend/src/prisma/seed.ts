import 'dotenv/config'
import { PrismaClient } from '@prisma/client'
import bcrypt from 'bcryptjs'

const prisma = new PrismaClient()

async function main() {
  const passwordHash = await bcrypt.hash('password123', 10)

  const admin = await prisma.user.upsert({
    where: { employeeNumber: 'ADMIN001' },
    update: {},
    create: {
      employeeNumber: 'ADMIN001',
      name: 'Admin User',
      email: 'admin@miaw.com',
      passwordHash,
      role: 'ADMIN',
      category: 'ADMINISTRATIVE'
    }
  })

  const manager = await prisma.user.upsert({
    where: { employeeNumber: 'MAN001' },
    update: {},
    create: {
      employeeNumber: 'MAN001',
      name: 'Manager User',
      email: 'manager@miaw.com',
      passwordHash,
      role: 'MANAGER',
      category: 'ADMINISTRATIVE'
    }
  })

  const employee = await prisma.user.upsert({
    where: { employeeNumber: 'EMP001' },
    update: {},
    create: {
      employeeNumber: 'EMP001',
      name: 'Employee User',
      email: 'employee@miaw.com',
      passwordHash,
      role: 'EMPLOYEE',
      category: 'VETERINARIAN'
    }
  })

  const morning = await prisma.shiftType.upsert({
    where: { name: 'Manhã' },
    update: {},
    create: {
      name: 'Manhã',
      startTime: '09:00',
      endTime: '13:00'
    }
  })

  const afternoon = await prisma.shiftType.upsert({
    where: { name: 'Tarde' },
    update: {},
    create: {
      name: 'Tarde',
      startTime: '14:00',
      endTime: '18:00'
    }
  })

  const night = await prisma.shiftType.upsert({
    where: { name: 'Noite' },
    update: {},
    create: {
      name: 'Noite',
      startTime: '18:00',
      endTime: '22:00'
    }
  })

  const users = [admin, manager, employee]
  const shiftTypes = [morning, afternoon, night]

  const dates = [
    '2026-04-27',
    '2026-04-28',
    '2026-04-29',
    '2026-04-30',
    '2026-05-01',
    '2026-05-02',
    '2026-05-03'
  ]

  for (let i = 0; i < users.length; i++) {
    for (let j = 0; j < dates.length; j++) {
      await prisma.shift.upsert({
        where: {
          userId_date: {
            userId: users[i].id,
            date: new Date(dates[j])
          }
        },
        update: {
          shiftTypeId: shiftTypes[(i + j) % shiftTypes.length].id
        },
        create: {
          userId: users[i].id,
          shiftTypeId: shiftTypes[(i + j) % shiftTypes.length].id,
          date: new Date(dates[j])
        }
      })
    }
  }

  console.log('Seed concluido com users, shift types e shifts.')
}

main()
  .catch(e => {
    console.error(e)
    process.exit(1)
  })
  .finally(async () => {
    await prisma.$disconnect()
  })
