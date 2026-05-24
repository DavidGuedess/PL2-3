import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import { getAttendance, getActiveEmployees } from "../../api/attendanceApi";
import { getUsers } from "../../api/userApi";

import type { ActiveEmployee, AttendanceRecord } from "../../models/attendance";
import type { User } from "../../models/user";

import { routes } from "../../navigation/routes";
import { tokenManager } from "../../storage/tokenManager";
import { getBackendErrorMessage } from "../../utils/errorUtils";

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
  return new Date(now.getFullYear(), now.getMonth(), 1)
    .toISOString()
    .slice(0, 10);
}

function formatDate(date: string): string {
  try {
    return new Intl.DateTimeFormat("pt-PT", {
      weekday: "short",
      day: "2-digit",
      month: "short",
      year: "numeric",
    }).format(new Date(date));
  } catch {
    return date;
  }
}

function formatTime(timestamp: string): string {
  try {
    return new Intl.DateTimeFormat("pt-PT", {
      hour: "2-digit",
      minute: "2-digit",
    }).format(new Date(timestamp));
  } catch {
    return timestamp;
  }
}

function formatMinutes(totalMinutes: number): string {
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

function getInitials(name: string): string {
  return (
    name
      .split(" ")
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join("") || "?"
  );
}

function calculateTotalMinutes(records: AttendanceRecord[]): number {
  let totalMinutes = 0;
  let lastIn: AttendanceRecord | null = null;

  const ordered = [...records].sort((a, b) =>
    a.timestamp.localeCompare(b.timestamp),
  );

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

  const ordered = [...records].sort((a, b) =>
    a.timestamp.localeCompare(b.timestamp),
  );

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
    groups.set(date, [...(groups.get(date) ?? []), record]);
  }

  return Array.from(groups.entries())
    .map(([date, dayRecords]) => ({
      date,
      records: [...dayRecords].sort((a, b) =>
        a.timestamp.localeCompare(b.timestamp),
      ),
      totalMinutes: calculateTotalMinutes(dayRecords),
      hasOpenRecord: hasOpenRecord(dayRecords),
    }))
    .sort((a, b) => b.date.localeCompare(a.date));
}

export function AttendanceMonitorScreen() {
  const navigate = useNavigate();
  const currentRole = tokenManager.getUserRole();
  const canViewMonitor = currentRole === "ADMIN" || currentRole === "MANAGER";

  const [users, setUsers] = useState<User[]>([]);
  const [activeEmployees, setActiveEmployees] = useState<ActiveEmployee[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [records, setRecords] = useState<AttendanceRecord[]>([]);
  const [fromDate, setFromDate] = useState(getMonthStart());
  const [toDate, setToDate] = useState(getToday());
  const [isLoadingUsers, setIsLoadingUsers] = useState(false);
  const [isLoadingAttendance, setIsLoadingAttendance] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const selectedUser = useMemo(() => {
    return users.find((user) => user.id === selectedUserId) ?? null;
  }, [users, selectedUserId]);

  const dailyAttendances = useMemo(() => {
    return buildDailyAttendances(records);
  }, [records]);

  const totalMinutes = useMemo(() => {
    return dailyAttendances.reduce((sum, day) => sum + day.totalMinutes, 0);
  }, [dailyAttendances]);

  async function loadUsers() {
    if (!canViewMonitor) {
      return;
    }

    try {
      setIsLoadingUsers(true);
      setErrorMessage(null);

      const [usersResult, activeResult] = await Promise.all([
        getUsers(),
        getActiveEmployees(),
      ]);

      setUsers(usersResult);
      setActiveEmployees(activeResult);

      if (usersResult.length > 0 && selectedUserId === null) {
        setSelectedUserId(usersResult[0].id);
      }
    } catch (error) {
      setErrorMessage(
        getBackendErrorMessage(error, "Erro ao carregar funcionários"),
      );
    } finally {
      setIsLoadingUsers(false);
    }
  }

  async function loadAttendance(userId: number | null = selectedUserId) {
    if (userId === null) {
      return;
    }

    try {
      setIsLoadingAttendance(true);
      setErrorMessage(null);

      const result = await getAttendance({
        userId,
        from: fromDate || null,
        to: toDate || null,
      });

      setRecords(result);
    } catch (error) {
      setErrorMessage(
        getBackendErrorMessage(
          error,
          "Erro ao carregar presenças do funcionário",
        ),
      );
    } finally {
      setIsLoadingAttendance(false);
    }
  }

  useEffect(() => {
    if (canViewMonitor) {
      void loadUsers();
    }
  }, [canViewMonitor]);

  useEffect(() => {
    if (selectedUserId !== null) {
      void loadAttendance(selectedUserId);
    }
  }, [selectedUserId]);

  if (!canViewMonitor) {
    return (
      <main className="monitor-page">
        <section className="monitor-shell">
          <header className="screen-header dark-header">
            <button
              className="secondary-button"
              onClick={() => navigate(routes.dashboard)}
            >
              Voltar
            </button>

            <div>
              <h1>Monitorização de Presenças</h1>
              <p>Área reservada à gestão</p>
            </div>
          </header>

          <section className="panel-card">
            <p className="muted-text">
              Não tens permissões para consultar a monitorização de presenças.
            </p>
          </section>
        </section>
      </main>
    );
  }
  return (
    <main className="monitor-page">
      <section className="monitor-shell">
        <header className="screen-header dark-header">
          <button
            className="secondary-button"
            onClick={() => navigate(routes.dashboard)}
          >
            Voltar
          </button>

          <div>
            <h1>Monitorização de Presenças</h1>
            <p>Consulta dos registos de presença por funcionário</p>
          </div>
        </header>

        {errorMessage && <div className="dashboard-error">{errorMessage}</div>}

        <section className="monitor-layout">
          <aside className="panel-card employee-panel">
            <div className="panel-header">
              <h2>Funcionários</h2>
            </div>

            {isLoadingUsers ? (
              <p className="muted-text">A carregar funcionários...</p>
            ) : users.length === 0 ? (
              <p className="muted-text">Sem funcionários.</p>
            ) : (
              <div className="employee-list">
                {users.map((user) => {
                  const isActiveNow = activeEmployees.some(
                    (active) => active.userId === user.id,
                  );
                  const isSelected = user.id === selectedUserId;

                  return (
                    <button
                      className={`employee-row ${isSelected ? "selected" : ""}`}
                      key={user.id}
                      onClick={() => setSelectedUserId(user.id)}
                    >
                      <span className="employee-avatar-small">
                        {getInitials(user.name)}
                      </span>

                      <span>
                        <strong>{user.name}</strong>
                        <small>{user.employeeNumber}</small>
                      </span>

                      {isActiveNow && <em>Ativo</em>}
                    </button>
                  );
                })}
              </div>
            )}
          </aside>

          <section className="monitor-main">
            <section className="attendance-summary-grid">
              <article className="summary-card">
                <span>Funcionário</span>
                <strong>{selectedUser?.name ?? "—"}</strong>
              </article>

              <article className="summary-card">
                <span>Dias com registo</span>
                <strong>{dailyAttendances.length}</strong>
              </article>

              <article className="summary-card">
                <span>Total no período</span>
                <strong>{formatMinutes(totalMinutes)}</strong>
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

              <button
                className="primary-button filter-button"
                onClick={() => loadAttendance()}
                disabled={isLoadingAttendance || selectedUserId === null}
              >
                {isLoadingAttendance ? "A carregar..." : "Aplicar filtros"}
              </button>
            </section>

            <section className="panel-card">
              <div className="panel-header">
                <h2>Registos</h2>
              </div>

              {isLoadingAttendance ? (
                <p className="muted-text">A carregar presenças...</p>
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
                          {day.hasOpenRecord
                            ? "Em aberto"
                            : formatMinutes(day.totalMinutes)}
                        </strong>
                      </div>

                      <div className="record-list">
                        {day.records.map((record) => (
                          <div className="record-row" key={record.id}>
                            <span
                              className={
                                record.type === "IN"
                                  ? "record-in"
                                  : "record-out"
                              }
                            >
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
        </section>
      </section>
    </main>
  );
}
