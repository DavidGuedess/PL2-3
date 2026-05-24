import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import { getMyAttendanceHistory } from "../../api/attendanceApi";
import type { AttendanceRecord } from "../../models/attendance";
import { routes } from "../../navigation/routes";

interface DailyAttendance {
  date: string;
  records: AttendanceRecord[];
  totalMinutes: number;
  hasOpenRecord: boolean;
}

function getToday(): string {
  return new Date().toISOString().slice(0, 10);
}

function getMonthStart(): string {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10);
}

function formatDate(date: string): string {
  try {
    return new Intl.DateTimeFormat("pt-PT", {
      weekday: "long",
      day: "2-digit",
      month: "long",
      year: "numeric"
    }).format(new Date(date));
  } catch {
    return date;
  }
}

function formatTime(timestamp: string): string {
  try {
    return new Intl.DateTimeFormat("pt-PT", {
      hour: "2-digit",
      minute: "2-digit"
    }).format(new Date(timestamp));
  } catch {
    return timestamp;
  }
}

function formatTotalMinutes(totalMinutes: number): string {
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;

  if (hours > 0) {
    return `${hours}h ${minutes}min`;
  }

  if (minutes > 0) {
    return `${minutes}min`;
  }

  return "0min";
}

function calculateTotalMinutes(records: AttendanceRecord[]): number {
  let totalMinutes = 0;
  let lastIn: AttendanceRecord | null = null;

  const ordered = [...records].sort((a, b) => a.timestamp.localeCompare(b.timestamp));

  for (const record of ordered) {
    if (record.type === "IN") {
      lastIn = record;
    } else if (record.type === "OUT" && lastIn) {
      const start = new Date(lastIn.timestamp).getTime();
      const end = new Date(record.timestamp).getTime();

      totalMinutes += Math.max(0, Math.floor((end - start) / 60000));
      lastIn = null;
    }
  }

  return totalMinutes;
}

function hasOpenRecord(records: AttendanceRecord[]): boolean {
  let lastIn: AttendanceRecord | null = null;

  const ordered = [...records].sort((a, b) => a.timestamp.localeCompare(b.timestamp));

  for (const record of ordered) {
    if (record.type === "IN") {
      lastIn = record;
    } else if (record.type === "OUT" && lastIn) {
      lastIn = null;
    }
  }

  return lastIn !== null;
}

function buildDailyAttendances(records: AttendanceRecord[]): DailyAttendance[] {
  const groups = new Map<string, AttendanceRecord[]>();

  for (const record of records) {
    const date = record.timestamp.slice(0, 10);
    const existing = groups.get(date) ?? [];
    groups.set(date, [...existing, record]);
  }

  return Array.from(groups.entries())
    .map(([date, dayRecords]) => ({
      date,
      records: [...dayRecords].sort((a, b) => a.timestamp.localeCompare(b.timestamp)),
      totalMinutes: calculateTotalMinutes(dayRecords),
      hasOpenRecord: hasOpenRecord(dayRecords)
    }))
    .sort((a, b) => b.date.localeCompare(a.date));
}

export function AttendanceHistoryScreen() {
  const navigate = useNavigate();

  const [records, setRecords] = useState<AttendanceRecord[]>([]);
  const [fromDate, setFromDate] = useState(getMonthStart());
  const [toDate, setToDate] = useState(getToday());
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const dailyAttendances = useMemo(() => {
    return buildDailyAttendances(records);
  }, [records]);

  const totalMinutes = useMemo(() => {
    return dailyAttendances.reduce((sum, day) => sum + day.totalMinutes, 0);
  }, [dailyAttendances]);

  async function loadHistory() {
    try {
      setIsLoading(true);
      setErrorMessage(null);

      const result = await getMyAttendanceHistory({
        from: fromDate || null,
        to: toDate || null
      });

      setRecords(result);
    } catch {
      setErrorMessage("Erro ao carregar histórico de presenças");
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    void loadHistory();
  }, []);

  return (
    <main className="attendance-page">
      <section className="attendance-shell">
        <header className="screen-header dark-header">
          <button className="secondary-button" onClick={() => navigate(routes.dashboard)}>
            Voltar
          </button>

          <div>
            <h1>Histórico de Presenças</h1>
            <p>Consulta dos registos de entrada e saída</p>
          </div>
        </header>

        <section className="attendance-summary-grid">
          <article className="summary-card">
            <span>Dias com registo</span>
            <strong>{dailyAttendances.length}</strong>
          </article>

          <article className="summary-card">
            <span>Total no período</span>
            <strong>{formatTotalMinutes(totalMinutes)}</strong>
          </article>

          <article className="summary-card">
            <span>Registos</span>
            <strong>{records.length}</strong>
          </article>
        </section>

        <section className="panel-card attendance-filters">
          <label>
            <span>De</span>
            <input
              type="date"
              value={fromDate}
              onChange={(event) => setFromDate(event.target.value)}
            />
          </label>

          <label>
            <span>Até</span>
            <input
              type="date"
              value={toDate}
              onChange={(event) => setToDate(event.target.value)}
            />
          </label>

          <button className="primary-button filter-button" onClick={loadHistory} disabled={isLoading}>
            {isLoading ? "A carregar..." : "Aplicar filtros"}
          </button>
        </section>

        {errorMessage && (
          <div className="dashboard-error">
            {errorMessage}
          </div>
        )}

        <section className="panel-card">
          <div className="panel-header">
            <h2>Registos por dia</h2>
          </div>

          {isLoading ? (
            <p className="muted-text">A carregar histórico...</p>
          ) : dailyAttendances.length === 0 ? (
            <p className="muted-text">Sem registos neste período.</p>
          ) : (
            <div className="daily-list">
              {dailyAttendances.map((day) => (
                <article className="daily-card" key={day.date}>
                  <div className="daily-card-header">
                    <div>
                      <h3>{formatDate(day.date)}</h3>
                      <p>{day.records.length} registo(s)</p>
                    </div>

                    <strong>
                      {day.hasOpenRecord ? "Em aberto" : formatTotalMinutes(day.totalMinutes)}
                    </strong>
                  </div>

                  <div className="record-list">
                    {day.records.map((record) => (
                      <div className="record-row" key={record.id}>
                        <span className={record.type === "IN" ? "record-in" : "record-out"}>
                          {record.type === "IN" ? "Entrada" : "Saída"}
                        </span>

                        <strong>{formatTime(record.timestamp)}</strong>

                        {record.note && <small>{record.note}</small>}
                      </div>
                    ))}
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </section>
    </main>
  );
}