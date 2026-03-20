import { PrismaClient } from '@prisma/client'
import bcrypt from 'bcryptjs'

const prisma = new PrismaClient()

async function main() {
  const passwordHash = await bcrypt.hash('password123', 10)

  await prisma.user.upsert({
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

  await prisma.user.upsert({
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

  await prisma.user.upsert({
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

  console.log('Seed concluido.')
}

main()
  .catch(e => {
    console.error(e)
    process.exit(1)
  })
  .finally(async () => {
    await prisma.$disconnect()
  })
