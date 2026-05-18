export interface AttendanceRecordLike {
  type: string
  timestamp: Date | string
}

export interface ShiftLike {
  date: Date | string
  shiftType: {
    startTime: string
    endTime: string
  }
}

export interface AttendanceStatsResult {
  totalHoursWorked: number
  daysWorked: number
  overtimeHours: number
}

export function shiftDurationHours(startTime: string, endTime: string): number {
  const [sh, sm] = startTime.split(':').map(Number)
  const [eh, em] = endTime.split(':').map(Number)
  const startMin = sh * 60 + sm
  const endMin = eh * 60 + em
  return endMin > startMin
    ? (endMin - startMin) / 60
    : (1440 - startMin + endMin) / 60
}

export function calculateAttendanceStats(
  records: AttendanceRecordLike[],
  shifts: ShiftLike[]
): AttendanceStatsResult {
  const sorted = [...records].sort(
    (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
  )

  const dailyWorkedMinutes = new Map<string, number>()
  let currentIn: Date | null = null

  for (const record of sorted) {
    const ts = new Date(record.timestamp)
    if (record.type === 'IN') {
      currentIn = ts
    } else if (record.type === 'OUT' && currentIn !== null) {
      const durationMin = (ts.getTime() - currentIn.getTime()) / 60000
      const dateKey = currentIn.toISOString().split('T')[0]
      dailyWorkedMinutes.set(dateKey, (dailyWorkedMinutes.get(dateKey) ?? 0) + durationMin)
      currentIn = null
    }
  }

  const totalHoursWorked = Array.from(dailyWorkedMinutes.values()).reduce(
    (sum, min) => sum + min / 60,
    0
  )
  const daysWorked = dailyWorkedMinutes.size

  const shiftDurationByDate = new Map<string, number>()
  for (const shift of shifts) {
    const dateKey = new Date(shift.date).toISOString().split('T')[0]
    shiftDurationByDate.set(dateKey, shiftDurationHours(shift.shiftType.startTime, shift.shiftType.endTime))
  }

  let overtimeHours = 0
  for (const [date, workedMin] of dailyWorkedMinutes) {
    const shiftHours = shiftDurationByDate.get(date)
    if (shiftHours !== undefined) {
      const overtime = workedMin / 60 - shiftHours
      if (overtime > 0) overtimeHours += overtime
    }
  }

  return {
    totalHoursWorked: Math.round(totalHoursWorked * 100) / 100,
    daysWorked,
    overtimeHours: Math.round(overtimeHours * 100) / 100
  }
}
