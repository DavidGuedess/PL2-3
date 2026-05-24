import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  getActiveEmployees,
  getMyAttendanceHistory,
  registerAttendance,
} from "../../api/attendanceApi";
import { getMyShifts, getShifts } from "../../api/shiftApi";
import { getShiftSwapRequests, getTimeOffRequests } from "../../api/requestApi";

import type { ActiveEmployee, AttendanceRecord } from "../../models/attendance";
import type { Shift } from "../../models/shift";
import type { ShiftSwapRequest, TimeOffRequest } from "../../models/request";

import {
  resolvedEndTime,
  resolvedName,
  resolvedStartTime,
} from "../../models/shift";
import { routes } from "../../navigation/routes";
import { tokenManager } from "../../storage/tokenManager";

import { DashboardRequestActions } from "./DashboardRequestActions";

interface DashboardState {
  shifts: Shift[];
  attendanceRecords: AttendanceRecord[];
  activeEmployees: ActiveEmployee[];
  timeOffRequests: TimeOffRequest[];
  shiftSwapRequests: ShiftSwapRequest[];
  allWeekShiftsCount: number;
  isLoading: boolean;
  error: string | null;
  isClocked: boolean;
  clockedInSince: string | null;
}

const initialState: DashboardState = {
  shifts: [],
  attendanceRecords: [],
  activeEmployees: [],
  timeOffRequests: [],
  shiftSwapRequests: [],
  allWeekShiftsCount: 0,
  isLoading: true,
  error: null,
  isClocked: false,
  clockedInSince: null,
};

function getWeekStart(date: Date): string {
  const copy = new Date(date);
  const day = copy.getDay();
  const diff = day === 0 ? -6 : 1 - day;

  copy.setDate(copy.getDate() + diff);

  return copy.toISOString().slice(0, 10);
}

function getToday(): string {
  return new Date().toISOString().slice(0, 10);
}

function getGreeting(): string {
  const hour = new Date().getHours();

  if (hour >= 5 && hour <= 11) {
    return "Bom dia";
  }

  if (hour >= 12 && hour <= 18) {
    return "Boa tarde";
  }

  return "Boa noite";
}

function formatDate(date: string): string {
  try {
    return new Intl.DateTimeFormat("pt-PT", {
      weekday: "short",
      day: "2-digit",
      month: "short",
    }).format(new Date(date));
  } catch {
    return date.slice(0, 10);
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

function calculateTodayWorked(records: AttendanceRecord[]): string {
  const today = getToday();

  const dayRecords = records
    .filter((record) => record.timestamp.slice(0, 10) === today)
    .sort((a, b) => a.timestamp.localeCompare(b.timestamp));

  let totalMinutes = 0;
  let lastIn: AttendanceRecord | null = null;

  for (const record of dayRecords) {
    if (record.type === "IN") {
      lastIn = record;
    } else if (record.type === "OUT" && lastIn) {
      const start = new Date(lastIn.timestamp).getTime();
      const end = new Date(record.timestamp).getTime();

      totalMinutes += Math.max(0, Math.floor((end - start) / 60000));
      lastIn = null;
    }
  }

  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;

  if (hours > 0) {
    return `${hours}h ${minutes}min`;
  }

  if (minutes > 0) {
    return `${minutes}min`;
  }

  return "—";
}

function getElapsedTimer(clockedInSince: string | null): string {
  if (!clockedInSince) {
    return "00:00:00";
  }

  const diffSeconds = Math.max(
    0,
    Math.floor((Date.now() - new Date(clockedInSince).getTime()) / 1000),
  );

  const hours = Math.floor(diffSeconds / 3600);
  const minutes = Math.floor((diffSeconds % 3600) / 60);
  const seconds = diffSeconds % 60;

  return [
    String(hours).padStart(2, "0"),
    String(minutes).padStart(2, "0"),
    String(seconds).padStart(2, "0"),
  ].join(":");
}

export function DashboardScreen() {
  const navigate = useNavigate();

  const currentRole = tokenManager.getUserRole();
  const canViewManagementScreens =
    currentRole === "ADMIN" || currentRole === "MANAGER";

  const [state, setState] = useState<DashboardState>(initialState);
  const [timer, setTimer] = useState("00:00:00");
  const [isClockActionLoading, setIsClockActionLoading] = useState(false);

  const fullName = tokenManager.getUserName() ?? "Utilizador";
  const firstName = fullName.split(" ")[0] || "Utilizador";
  const currentUserId = tokenManager.getUserId();

  const today = getToday();
  const weekStart = useMemo(() => getWeekStart(new Date()), []);

  const todayShift = useMemo(() => {
    return (
      state.shifts.find((shift) => {
        return shift.published && shift.date.slice(0, 10) === today;
      }) ?? null
    );
  }, [state.shifts, today]);

  const pendingRequestsCount = useMemo(() => {
    const myTimeOff = state.timeOffRequests.filter((request) => {
      return request.userId === currentUserId && request.status === "PENDING";
    });

    const mySwaps = state.shiftSwapRequests.filter((request) => {
      return (
        request.requesterId === currentUserId && request.status === "PENDING"
      );
    });

    return myTimeOff.length + mySwaps.length;
  }, [state.timeOffRequests, state.shiftSwapRequests, currentUserId]);

  const publishedWeekShifts = useMemo(() => {
    return state.shifts
      .filter((shift) => shift.published)
      .sort((a, b) => a.date.localeCompare(b.date));
  }, [state.shifts]);

  async function loadDashboard() {
    try {
      setState((current) => ({
        ...current,
        isLoading: true,
        error: null,
      }));

      const weekEndDate = new Date(weekStart);
      weekEndDate.setDate(weekEndDate.getDate() + 6);
      const weekEnd = weekEndDate.toISOString().slice(0, 10);

      const [
        myShiftsResult,
        allShiftsResult,
        attendanceResult,
        activeEmployeesResult,
        timeOffResult,
        swapsResult,
      ] = await Promise.allSettled([
        getMyShifts({ week: weekStart }),
        getShifts({ week: weekStart }),
        getMyAttendanceHistory({ from: weekStart, to: weekEnd }),
        getActiveEmployees(),
        getTimeOffRequests(),
        getShiftSwapRequests(),
      ]);

      const myShifts =
        myShiftsResult.status === "fulfilled" ? myShiftsResult.value : [];

      const allShifts =
        allShiftsResult.status === "fulfilled" ? allShiftsResult.value : [];

      const attendanceRecords =
        attendanceResult.status === "fulfilled" ? attendanceResult.value : [];

      const activeEmployees =
        activeEmployeesResult.status === "fulfilled"
          ? activeEmployeesResult.value
          : [];

      const timeOffRequests =
        timeOffResult.status === "fulfilled" ? timeOffResult.value : [];

      const shiftSwapRequests =
        swapsResult.status === "fulfilled" ? swapsResult.value : [];

      const todayRecords = attendanceRecords
        .filter((record) => record.timestamp.slice(0, 10) === today)
        .sort((a, b) => a.timestamp.localeCompare(b.timestamp));

      const lastRecord =
        todayRecords.length > 0
          ? todayRecords[todayRecords.length - 1]
          : undefined;

      const isClocked = lastRecord?.type === "IN";

      setState({
        shifts: myShifts,
        attendanceRecords,
        activeEmployees,
        timeOffRequests,
        shiftSwapRequests,
        allWeekShiftsCount: allShifts.filter((shift) => shift.published).length,
        isLoading: false,
        error: null,
        isClocked,
        clockedInSince: isClocked ? (lastRecord?.timestamp ?? null) : null,
      });
    } catch {
      setState((current) => ({
        ...current,
        isLoading: false,
        error: "Erro ao carregar dashboard",
      }));
    }
  }

  async function handleClockIn() {
    try {
      setIsClockActionLoading(true);

      const record = await registerAttendance({
        type: "IN",
      });

      setState((current) => ({
        ...current,
        isClocked: true,
        clockedInSince: record.timestamp,
        attendanceRecords: [record, ...current.attendanceRecords],
      }));
    } catch {
      setState((current) => ({
        ...current,
        error: "Erro ao registar entrada",
      }));
    } finally {
      setIsClockActionLoading(false);
    }
  }

  async function handleClockOut() {
    try {
      setIsClockActionLoading(true);

      const record = await registerAttendance({
        type: "OUT",
      });

      setState((current) => ({
        ...current,
        isClocked: false,
        clockedInSince: null,
        attendanceRecords: [record, ...current.attendanceRecords],
      }));
    } catch {
      setState((current) => ({
        ...current,
        error: "Erro ao registar saída",
      }));
    } finally {
      setIsClockActionLoading(false);
    }
  }

  function handleLogout() {
    tokenManager.clearTokens();
    navigate(routes.login, { replace: true });
  }

  useEffect(() => {
    void loadDashboard();
  }, []);

  useEffect(() => {
    setTimer(getElapsedTimer(state.clockedInSince));

    const interval = window.setInterval(() => {
      setTimer(getElapsedTimer(state.clockedInSince));
    }, 1000);

    return () => {
      window.clearInterval(interval);
    };
  }, [state.clockedInSince]);

  return (
    <main className="dashboard-page">
      <section className="dashboard-shell">
        <header className="dashboard-header">
          <button
            className="avatar-button"
            onClick={() => navigate(routes.profile)}
          >
            {firstName.charAt(0).toUpperCase()}
          </button>

          <div>
            <h1>
              {getGreeting()}, {firstName}!
            </h1>
            <p>MiauGenda Desktop</p>
          </div>

          <button className="secondary-button" onClick={handleLogout}>
            Terminar sessão
          </button>
        </header>

        {state.error && <div className="dashboard-error">{state.error}</div>}

        {state.isLoading ? (
          <section className="dashboard-loading">
            A carregar dashboard...
          </section>
        ) : (
          <>
            <section className="clock-card">
              <div>
                <span className="section-kicker">Registo de ponto</span>

                {todayShift ? (
                  <>
                    <h2>
                      {resolvedStartTime(todayShift)} -{" "}
                      {resolvedEndTime(todayShift)}
                    </h2>
                    <p>{resolvedName(todayShift)}</p>
                  </>
                ) : (
                  <>
                    <h2>Sem turno agendado hoje</h2>
                    <p>A entrada será registada como não programada.</p>
                  </>
                )}

                {state.isClocked && state.clockedInSince && (
                  <p className="clock-status">
                    Entrada registada às {formatTime(state.clockedInSince)} ·{" "}
                    {timer}
                  </p>
                )}
              </div>

              {state.isClocked ? (
                <button
                  className="danger-button"
                  onClick={handleClockOut}
                  disabled={isClockActionLoading}
                >
                  {isClockActionLoading ? "A processar..." : "Registar Saída"}
                </button>
              ) : (
                <button
                  className="primary-button dashboard-primary"
                  onClick={handleClockIn}
                  disabled={isClockActionLoading}
                >
                  {isClockActionLoading ? "A processar..." : "Registar Entrada"}
                </button>
              )}
            </section>

            <section className="dashboard-grid">
              <article
                className="summary-card"
                onClick={() => navigate(routes.team)}
              >
                <span>Em turno agora</span>
                <strong>{state.activeEmployees.length}</strong>
              </article>

              <article
                className="summary-card"
                onClick={() => navigate(routes.scheduler)}
              >
                <span>Turno de hoje</span>
                <strong>
                  {todayShift
                    ? `${resolvedStartTime(todayShift).slice(0, 5)} - ${resolvedEndTime(todayShift).slice(0, 5)}`
                    : "—"}
                </strong>
              </article>

              <article
                className="summary-card"
                onClick={() => navigate(routes.scheduler)}
              >
                <span>Turnos esta semana</span>
                <strong>{state.allWeekShiftsCount}</strong>
              </article>

              <article
                className="summary-card"
                onClick={() => navigate(routes.notifications)}
              >
                <span>Pedidos pendentes</span>
                <strong>
                  {pendingRequestsCount > 0 ? pendingRequestsCount : "—"}
                </strong>
              </article>

              <article className="summary-card">
                <span>Total hoje</span>
                <strong>{calculateTodayWorked(state.attendanceRecords)}</strong>
              </article>
            </section>

            <section className="dashboard-content-grid">
              <article className="panel-card">
                <div className="panel-header">
                  <h2>Os meus turnos</h2>
                  <button onClick={() => navigate(routes.scheduler)}>
                    Ver agenda
                  </button>
                </div>

                {publishedWeekShifts.length === 0 ? (
                  <p className="muted-text">
                    Sem turnos publicados esta semana.
                  </p>
                ) : (
                  <div className="shift-list">
                    {publishedWeekShifts.slice(0, 5).map((shift) => (
                      <div className="shift-row" key={shift.id}>
                        <div className="shift-date">
                          <strong>{new Date(shift.date).getDate()}</strong>
                          <span>{formatDate(shift.date).slice(0, 3)}</span>
                        </div>

                        <div className="shift-info">
                          <span>{shift.shiftType?.name ?? "Sem posição"}</span>
                          <strong>
                            {resolvedStartTime(shift).slice(0, 5)} -{" "}
                            {resolvedEndTime(shift).slice(0, 5)}
                          </strong>
                        </div>

                        {shift.date.slice(0, 10) === today && (
                          <span className="today-badge">Hoje</span>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </article>

              <DashboardRequestActions
                myShifts={state.shifts}
                currentUserId={currentUserId}
                weekStart={weekStart}
                onRequestCreated={loadDashboard}
              />

              <article className="panel-card">
                <div className="panel-header">
                  <h2>Navegação</h2>
                </div>

                <div className="nav-grid">
                  <button onClick={() => navigate(routes.profile)}>
                    Perfil
                  </button>

                  <button onClick={() => navigate(routes.scheduler)}>
                    Agenda
                  </button>

                  <button onClick={() => navigate(routes.inbox)}>Inbox</button>

                  <button onClick={() => navigate(routes.notifications)}>
                    Notificações
                  </button>

                  <button onClick={() => navigate(routes.attendanceHistory)}>
                    Histórico
                  </button>

                  <button onClick={() => navigate(routes.availability)}>
                    Disponibilidade
                  </button>

                  {canViewManagementScreens && (
                    <>
                      <button onClick={() => navigate(routes.team)}>
                        Equipa
                      </button>

                      <button
                        onClick={() => navigate(routes.attendanceMonitor)}
                      >
                        Monitorização
                      </button>
                      <button onClick={() => navigate(routes.weekAssignments)}>
                        Atribuições
                      </button>
                    </>
                  )}
                </div>
              </article>
            </section>
          </>
        )}
      </section>
    </main>
  );
}
