import { shiftDurationHours, calculateAttendanceStats } from '../utils/attendanceStats'

describe('shiftDurationHours', () => {
  it('calcula duração de turno diurno', () => {
    expect(shiftDurationHours('08:00', '16:00')).toBe(8)
  })

  it('calcula duração de turno noturno (atravessa meia-noite)', () => {
    expect(shiftDurationHours('22:00', '06:00')).toBe(8)
  })

  it('calcula duração com minutos', () => {
    expect(shiftDurationHours('08:30', '17:30')).toBe(9)
  })

  it('calcula turno de 12 horas', () => {
    expect(shiftDurationHours('07:00', '19:00')).toBe(12)
  })
})

describe('calculateAttendanceStats', () => {
  it('retorna zeros quando não há registos', () => {
    const result = calculateAttendanceStats([], [])
    expect(result).toEqual({ totalHoursWorked: 0, daysWorked: 0, overtimeHours: 0 })
  })

  it('ignora registo IN sem OUT correspondente', () => {
    const records = [
      { type: 'IN', timestamp: new Date('2026-04-10T08:00:00Z') }
    ]
    const result = calculateAttendanceStats(records, [])
    expect(result).toEqual({ totalHoursWorked: 0, daysWorked: 0, overtimeHours: 0 })
  })

  it('calcula horas para um par IN/OUT', () => {
    const records = [
      { type: 'IN', timestamp: new Date('2026-04-10T08:00:00Z') },
      { type: 'OUT', timestamp: new Date('2026-04-10T16:00:00Z') }
    ]
    const result = calculateAttendanceStats(records, [])
    expect(result.totalHoursWorked).toBe(8)
    expect(result.daysWorked).toBe(1)
    expect(result.overtimeHours).toBe(0)
  })

  it('calcula horas para múltiplos pares no mesmo dia', () => {
    const records = [
      { type: 'IN', timestamp: new Date('2026-04-10T08:00:00Z') },
      { type: 'OUT', timestamp: new Date('2026-04-10T12:00:00Z') },
      { type: 'IN', timestamp: new Date('2026-04-10T13:00:00Z') },
      { type: 'OUT', timestamp: new Date('2026-04-10T17:00:00Z') }
    ]
    const result = calculateAttendanceStats(records, [])
    expect(result.totalHoursWorked).toBe(8)
    expect(result.daysWorked).toBe(1)
  })

  it('conta dias trabalhados corretamente em múltiplos dias', () => {
    const records = [
      { type: 'IN', timestamp: new Date('2026-04-10T08:00:00Z') },
      { type: 'OUT', timestamp: new Date('2026-04-10T16:00:00Z') },
      { type: 'IN', timestamp: new Date('2026-04-11T08:00:00Z') },
      { type: 'OUT', timestamp: new Date('2026-04-11T16:00:00Z') }
    ]
    const result = calculateAttendanceStats(records, [])
    expect(result.totalHoursWorked).toBe(16)
    expect(result.daysWorked).toBe(2)
  })

  it('calcula horas extra quando se trabalha além do turno', () => {
    const records = [
      { type: 'IN', timestamp: new Date('2026-04-10T08:00:00Z') },
      { type: 'OUT', timestamp: new Date('2026-04-10T18:00:00Z') }
    ]
    const shifts = [
      { date: new Date('2026-04-10T00:00:00Z'), shiftType: { startTime: '08:00', endTime: '16:00' } }
    ]
    const result = calculateAttendanceStats(records, shifts)
    expect(result.totalHoursWorked).toBe(10)
    expect(result.overtimeHours).toBe(2)
  })

  it('não contabiliza horas extra quando trabalha menos do turno', () => {
    const records = [
      { type: 'IN', timestamp: new Date('2026-04-10T08:00:00Z') },
      { type: 'OUT', timestamp: new Date('2026-04-10T14:00:00Z') }
    ]
    const shifts = [
      { date: new Date('2026-04-10T00:00:00Z'), shiftType: { startTime: '08:00', endTime: '16:00' } }
    ]
    const result = calculateAttendanceStats(records, shifts)
    expect(result.totalHoursWorked).toBe(6)
    expect(result.overtimeHours).toBe(0)
  })

  it('não conta horas extra em dias sem turno atribuído', () => {
    const records = [
      { type: 'IN', timestamp: new Date('2026-04-10T08:00:00Z') },
      { type: 'OUT', timestamp: new Date('2026-04-10T20:00:00Z') }
    ]
    const result = calculateAttendanceStats(records, [])
    expect(result.totalHoursWorked).toBe(12)
    expect(result.overtimeHours).toBe(0)
  })

  it('acumula horas extra de múltiplos dias', () => {
    const records = [
      { type: 'IN', timestamp: new Date('2026-04-10T08:00:00Z') },
      { type: 'OUT', timestamp: new Date('2026-04-10T18:00:00Z') },
      { type: 'IN', timestamp: new Date('2026-04-11T08:00:00Z') },
      { type: 'OUT', timestamp: new Date('2026-04-11T17:00:00Z') }
    ]
    const shifts = [
      { date: new Date('2026-04-10T00:00:00Z'), shiftType: { startTime: '08:00', endTime: '16:00' } },
      { date: new Date('2026-04-11T00:00:00Z'), shiftType: { startTime: '08:00', endTime: '16:00' } }
    ]
    const result = calculateAttendanceStats(records, shifts)
    expect(result.totalHoursWorked).toBe(19)
    expect(result.overtimeHours).toBe(3)
  })

  it('processa registos desordenados corretamente', () => {
    const records = [
      { type: 'OUT', timestamp: new Date('2026-04-10T16:00:00Z') },
      { type: 'IN', timestamp: new Date('2026-04-10T08:00:00Z') }
    ]
    const result = calculateAttendanceStats(records, [])
    expect(result.totalHoursWorked).toBe(8)
    expect(result.daysWorked).toBe(1)
  })

  it('arredonda horas a 2 casas decimais', () => {
    const records = [
      { type: 'IN', timestamp: new Date('2026-04-10T08:00:00Z') },
      { type: 'OUT', timestamp: new Date('2026-04-10T16:10:00Z') }
    ]
    const result = calculateAttendanceStats(records, [])
    expect(result.totalHoursWorked).toBe(8.17)
  })
})
