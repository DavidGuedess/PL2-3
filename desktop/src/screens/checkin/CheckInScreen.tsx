import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  getMyAttendanceHistory,
  registerAttendance
} from "../../api/attendanceApi";
import { getMyShifts } from "../../api/shiftApi";

import type { AttendanceRecord } from "../../models/attendance";
import type { Shift } from "../../models/shift";

import {
  resolvedEndTime,
  resolvedName,
  resolvedStartTime
} from "../../models/shift";
import { routes } from "../../navigation/routes";

function getToday(): string {
  return new Date().toISOString().slice(0, 10);
}

function getWeekStart(date: Date): string {
  const copy = new Date(date);
  const day = copy.getDay();
  const diff = day === 0 ? -6 : 1 - day;

  copy.setDate(copy.getDate() + diff);

  return copy.toISOString().slice(0, 10);
}

function formatDate(timestamp: string): string {
  try {
    return new Intl.DateTimeFormat("pt-PT", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric"
    }).format(new Date(timestamp));
  } catch {
    return timestamp;
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

function formatType(type: string): string {
  return type === "IN" ? "Entrada" : "Saída";
}

export function CheckInScreen() {
  const navigate = useNavigate();

  const [isWorking, setIsWorking] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [records, setRecords] = useState<AttendanceRecord[]>([]);
  const [todayShift, setTodayShift] = useState<Shift | null>(null);

  const today = useMemo(() => getToday(), []);
  const weekStart = useMemo(() => getWeekStart(new Date()), []);

  async function loadTodayShift() {
    try {
      const shifts = await getMyShifts({ week: weekStart });

      const shift = shifts.find((item) => {
        return item.published && item.date.slice(0, 10) === today;
      });

      setTodayShift(shift ?? null);
    } catch {
      setTodayShift(null);
    }
  }

  async function loadMyAttendanceHistory() {
    try {
      setIsLoading(true);
      setMessage(null);

      const history = await getMyAttendanceHistory();

      const ordered = [...history].sort((a, b) => {
        return b.timestamp.localeCompare(a.timestamp);
      });

      const latestRecord = ordered.length > 0 ? ordered[0] : undefined;

      setRecords(ordered);
      setIsWorking(latestRecord?.type === "IN");
    } catch {
      setMessage("Erro ao carregar histórico");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleRegisterIn() {
    try {
      setIsLoading(true);
      setMessage(null);

      await registerAttendance({ type: "IN" });

      setMessage("Entrada registada com sucesso");
      await loadMyAttendanceHistory();
    } catch {
      setMessage("Erro ao registar entrada");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleRegisterOut() {
    try {
      setIsLoading(true);
      setMessage(null);

      await registerAttendance({ type: "OUT" });

      setMessage("Saída registada com sucesso");
      await loadMyAttendanceHistory();
    } catch {
      setMessage("Erro ao registar saída");
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    void loadMyAttendanceHistory();
    void loadTodayShift();
  }, []);

  return (
    <main className="checkin-page">
      <section className="checkin-shell">
        <header className="screen-header">
          <button className="secondary-button" onClick={() => navigate(routes.dashboard)}>
            Voltar
          </button>

          <div>
            <h1>MIAUGENDA</h1>
            <p>Registo de presença</p>
          </div>
        </header>

        <section className="checkin-card">
          <span className="section-kicker">Turno atual</span>

          <div className="current-shift-box">
            {todayShift ? (
              <>
                <div className="info-line">
                  <strong>Data:</strong>
                  <span>{formatDate(todayShift.date)}</span>
                </div>

                <div className="info-line">
                  <strong>Horário:</strong>
                  <span>
                    {resolvedStartTime(todayShift)} - {resolvedEndTime(todayShift)}
                  </span>
                </div>

                <div className="info-line">
                  <strong>Posição:</strong>
                  <span>{resolvedName(todayShift)}</span>
                </div>
              </>
            ) : (
              <div className="info-line">
                <strong>Estado:</strong>
                <span>Sem turno agendado para hoje</span>
              </div>
            )}
          </div>

          <div className="checkin-actions">
            <button
              className="success-button"
              onClick={handleRegisterIn}
              disabled={isWorking || isLoading}
            >
              {isLoading ? "A processar..." : "Registar Entrada"}
            </button>

            <button
              className="success-button"
              onClick={handleRegisterOut}
              disabled={!isWorking || isLoading}
            >
              {isLoading ? "A processar..." : "Registar Saída"}
            </button>
          </div>

          {message && (
            <div className="checkin-message">
              {message}
            </div>
          )}
        </section>

        <section className="panel-card">
          <div className="panel-header">
            <h2>Histórico de registos</h2>
          </div>

          {records.length === 0 ? (
            <p className="muted-text">
              Ainda não existem registos de presença.
            </p>
          ) : (
            <div className="attendance-list">
              {records.slice(0, 5).map((record) => (
                <div className="attendance-row" key={record.id}>
                  <div>
                    <strong>{formatDate(record.timestamp)}</strong>
                    <span>Registo de presença</span>
                  </div>

                  <p>
                    {formatType(record.type)}: {formatTime(record.timestamp)}
                  </p>
                </div>
              ))}
            </div>
          )}
        </section>
      </section>
    </main>
  );
}