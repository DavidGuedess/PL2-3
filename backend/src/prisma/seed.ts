import { PrismaClient, Role, ShiftType } from '@prisma/client'
import bcrypt from 'bcryptjs'

const prisma = new PrismaClient()

async function main() {
  console.log('Seeding database...')

  const adminHash = await bcrypt.hash('admin123', 10)
  await prisma.user.upsert({
    where: { employeeNumber: 'ADM001' },
    update: {},
    create: {
      employeeNumber: 'ADM001',
      fullName: 'Administrador',
      email: 'admin@pawschedule.pt',
      passwordHash: adminHash,
      role: Role.ADMIN,
      department: 'Administracao',
    },
  })

  const managerHash = await bcrypt.hash('manager123', 10)
  await prisma.user.upsert({
    where: { employeeNumber: 'GER001' },
    update: {},
    create: {
      employeeNumber: 'GER001',
      fullName: 'Gerente Silva',
      email: 'gerente@pawschedule.pt',
      passwordHash: managerHash,
      role: Role.MANAGER,
      department: 'Gestao',
    },
  })

  await prisma.shiftTemplate.createMany({
    skipDuplicates: true,
    data: [
      { name: 'Manha', shiftType: ShiftType.MORNING, startTime: '08:00', endTime: '16:00' },
      { name: 'Tarde', shiftType: ShiftType.AFTERNOON, startTime: '16:00', endTime: '00:00' },
      { name: 'Noite', shiftType: ShiftType.NIGHT, startTime: '00:00', endTime: '08:00' },
    ],
  })

  console.log('Seed complete!')
  console.log('Admin:   ADM001 / admin123')
  console.log('Manager: GER001 / manager123')
}

main()
  .catch(console.error)
  .finally(() => prisma.$disconnect())
