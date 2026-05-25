import { useEffect, useMemo, useState } from "react";
import {
  Clock,
  Coffee,
  Timer,
  ClipboardList,
  CalendarDays,
  User,
  Unlock,
  AlertTriangle,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { useNavigate } from "react-router-dom";

import {
  getActiveEmployees,
  getMyAttendanceHistory,
} from "../../api/attendanceApi";
import { getMyShifts, getShifts } from "../../api/shiftApi";
import { getTimeOffRequests, getShiftSwapRequests } from "../../api/requestApi";
import { resolvedStartTime, resolvedEndTime } from "../../models/shift";
import type { Shift } from "../../models/shift";
import type { TimeOffRequest, ShiftSwapRequest } from "../../models/request";
import type { AttendanceRecord } from "../../models/attendance";
import { routes } from "../../navigation/routes";
import { tokenManager } from "../../storage/tokenManager";

function getToday() {
  return new Date().toISOString().slice(0, 10);
}

function mondayOf(date: Date): string {
  const d = new Date(date);
  const day = d.getDay();
  d.setDate(d.getDate() + (day === 0 ? -6 : 1 - day));
  return d.toISOString().slice(0, 10);
}

function addWeeks(base: string, n: number): string {
  const d = new Date(base);
  d.setDate(d.getDate() + n * 7);
  return d.toISOString().slice(0, 10);
}

function weekEnd(start: string): string {
  const d = new Date(start);
  d.setDate(d.getDate() + 6);
  return d.toISOString().slice(0, 10);
}

function fmtWeek(start: string): string {
  const end = weekEnd(start);
  return `${start.slice(8, 10)} — ${end.slice(8, 10)} de ${new Intl.DateTimeFormat(
    "pt-PT",
    { month: "short" },
  ).format(new Date(end + "T12:00"))}. ${end.slice(0, 4)}`;
}

function fmtDate(date: string) {
  return new Intl.DateTimeFormat("pt-PT", {
    weekday: "short",
    day: "2-digit",
    month: "short",
  }).format(new Date(date.slice(0, 10) + "T12:00"));
}

function fmtTime(ts: string) {
  return new Intl.DateTimeFormat("pt-PT", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(ts));
}

function greeting() {
  const h = new Date().getHours();
  if (h >= 5 && h <= 11) return "Bom dia";
  if (h >= 12 && h <= 18) return "Boa tarde";
  return "Boa noite";
}

function calcWorked(records: AttendanceRecord[]): string {
  const today = getToday();
  const sorted = records
    .filter((r) => r.timestamp.slice(0, 10) === today)
    .sort((a, b) => a.timestamp.localeCompare(b.timestamp));
  let mins = 0,
    lastIn: string | null = null;
  for (const r of sorted) {
    if (r.type === "IN") lastIn = r.timestamp;
    else if (r.type === "OUT" && lastIn) {
      mins += Math.max(
        0,
        Math.floor(
          (new Date(r.timestamp).getTime() - new Date(lastIn).getTime()) /
            60000,
        ),
      );
      lastIn = null;
    }
  }
  const h = Math.floor(mins / 60),
    m = mins % 60;
  if (h > 0) return `${h}h ${m}min`;
  if (m > 0) return `${m}min`;
  return "—";
}

function statusLabel(s: string) {
  if (s === "PENDING") return "Pendente";
  if (s === "APPROVED") return "Aprovado";
  if (s === "REJECTED") return "Rejeitado";
  return s;
}

function parseTodayTime(time: string): Date | null {
  const [h, m] = time.slice(0, 5).split(":").map(Number);

  if (Number.isNaN(h) || Number.isNaN(m)) {
    return null;
  }

  const d = new Date();
  d.setHours(h, m, 0, 0);
  return d;
}

function addMinutes(date: Date, minutes: number): Date {
  const d = new Date(date);
  d.setMinutes(d.getMinutes() + minutes);
  return d;
}

export function DashboardScreen() {
  const navigate = useNavigate();
  const userId = tokenManager.getUserId();
  const name = tokenManager.getUserName() ?? "Utilizador";
  const firstName = name.split(" ")[0];

  const today = getToday();
  const [weekStart, setWeekStart] = useState(mondayOf(new Date()));

  const [shifts, setShifts] = useState<Shift[]>([]);
  const [attendance, setAttendance] = useState<AttendanceRecord[]>([]);
  const [activeCount, setActiveCount] = useState(0);
  const [timeOffs, setTimeOffs] = useState<TimeOffRequest[]>([]);
  const [swaps, setSwaps] = useState<ShiftSwapRequest[]>([]);
  const [allWeekCount, setAllWeekCount] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [now, setNow] = useState(new Date());

  const wEnd = weekEnd(weekStart);

  async function load() {
    setIsLoading(true);
    try {
      const [s, allS, att, active, tor, ssr] = await Promise.allSettled([
        getMyShifts({ week: weekStart }),
        getShifts({ week: weekStart }),
        getMyAttendanceHistory({ from: weekStart, to: wEnd }),
        getActiveEmployees(),
        getTimeOffRequests(),
        getShiftSwapRequests(),
      ]);
      setShifts(s.status === "fulfilled" ? s.value : []);
      setAllWeekCount(
        allS.status === "fulfilled"
          ? allS.value.filter((x) => x.published).length
          : 0,
      );
      setAttendance(att.status === "fulfilled" ? att.value : []);
      setActiveCount(active.status === "fulfilled" ? active.value.length : 0);
      setTimeOffs(tor.status === "fulfilled" ? tor.value : []);
      setSwaps(ssr.status === "fulfilled" ? ssr.value : []);
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, [weekStart]);

  useEffect(() => {
    const id = window.setInterval(() => setNow(new Date()), 30_000);
    return () => window.clearInterval(id);
  }, []);

  const todayShift = useMemo(
    () =>
      shifts.find((s) => s.published && s.date.slice(0, 10) === today) ?? null,
    [shifts, today],
  );

  const canRegisterPoint = useMemo(() => {
    if (!todayShift) {
      return false;
    }

    const shiftStart = parseTodayTime(resolvedStartTime(todayShift));
    const shiftEnd = parseTodayTime(resolvedEndTime(todayShift));

    if (!shiftStart || !shiftEnd) {
      return false;
    }

    return now >= shiftStart && now <= addMinutes(shiftEnd, 15);
  }, [todayShift, now]);

  const publishedWeekShifts = useMemo(
    () =>
      shifts
        .filter((s) => s.published)
        .sort((a, b) => a.date.localeCompare(b.date)),
    [shifts],
  );

  const myTimeOffs = timeOffs.filter((r) => r.userId === userId);
  const mySwaps = swaps.filter((r) => r.requesterId === userId);
  const pendingCount =
    myTimeOffs.filter((r) => r.status === "PENDING").length +
    mySwaps.filter((r) => r.status === "PENDING").length;

  const todayTimeOffs = timeOffs.filter(
    (r) =>
      r.status === "APPROVED" &&
      new Date(r.startDate) <= new Date(today) &&
      new Date(r.endDate) >= new Date(today),
  ).length;

  const worked = calcWorked(attendance);

  return (
    <>
      <div className="dashboard-greeting">
        <h1>
          {greeting()}, {firstName}!
        </h1>
        <p>
          MiauGenda ·{" "}
          {new Intl.DateTimeFormat("pt-PT", {
            weekday: "long",
            day: "numeric",
            month: "long",
          }).format(new Date())}
        </p>
      </div>

      {isLoading ? (
        <div className="loading-state">A carregar...</div>
      ) : (
        <>
          {/* Summary row */}
          <div className="dashboard-summary-grid">
            {/* Today panel */}
            <div className="summary-panel">
              <div className="summary-panel-header">
                <h2>Resumo de hoje</h2>
              </div>

              <div className="stat-card" onClick={() => navigate(routes.team)}>
                <div className="stat-icon">
                  <Clock size={20} color="white" />
                </div>
                <div className="stat-content">
                  <div className="stat-label">
                    Atualmente com ponto registado
                  </div>
                  <div className="stat-value">{activeCount}</div>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon">
                  <Coffee size={20} color="white" />
                </div>
                <div className="stat-content">
                  <div className="stat-label">Folgas hoje</div>
                  <div className="stat-value">{todayTimeOffs}</div>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon">
                  <Timer size={20} color="white" />
                </div>
                <div className="stat-content">
                  <div className="stat-label">Tempo trabalhado hoje</div>
                  <div className="stat-value" style={{ fontSize: 20 }}>
                    {worked}
                  </div>
                </div>
              </div>

              <div
                className="stat-card"
                onClick={() => navigate(routes.notifications)}
              >
                <div className="stat-icon">
                  <ClipboardList size={20} color="white" />
                </div>
                <div className="stat-content">
                  <div className="stat-label">Pedidos pendentes</div>
                  <div className="stat-value">{pendingCount}</div>
                </div>
              </div>
            </div>

            {/* Week panel */}
            <div className="summary-panel">
              <div className="summary-panel-header">
                <h2>Resumo da semana</h2>
                <div className="summary-week-nav">
                  <button onClick={() => setWeekStart(addWeeks(weekStart, -1))}>
                    <ChevronLeft size={16} />
                  </button>
                  <span>{fmtWeek(weekStart)}</span>
                  <button onClick={() => setWeekStart(addWeeks(weekStart, 1))}>
                    <ChevronRight size={16} />
                  </button>
                </div>
              </div>

              <div
                className="stat-card"
                onClick={() => navigate(routes.scheduler)}
              >
                <div className="stat-icon">
                  <CalendarDays size={20} color="white" />
                </div>
                <div className="stat-content">
                  <div className="stat-label">Total de turnos agendados</div>
                  <div className="stat-value">{allWeekCount}</div>
                </div>
                <span
                  className="stat-link"
                  style={{ display: "flex", alignItems: "center", gap: 2 }}
                >
                  Ver agenda <ChevronRight size={13} />
                </span>
              </div>

              <div
                className="stat-card"
                onClick={() => navigate(routes.notifications)}
              >
                <div className="stat-icon">
                  <User size={20} color="white" />
                </div>
                <div className="stat-content">
                  <div className="stat-label">Aguardando confirmação</div>
                  <div className="stat-value">{pendingCount}</div>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon">
                  <Unlock size={20} color="white" />
                </div>
                <div className="stat-content">
                  <div className="stat-label">Turnos em aberto</div>
                  <div className="stat-value">0</div>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon">
                  <AlertTriangle size={20} color="white" />
                </div>
                <div className="stat-content">
                  <div className="stat-label">Turnos inacabados</div>
                  <div className="stat-value">0</div>
                </div>
              </div>
            </div>
          </div>

          {/* Bottom panels */}
          <div className="dashboard-bottom">
            {/* My shifts */}
            <div className="panel-card">
              <div className="panel-card-header">
                <h3>Os meus turnos</h3>
                <button onClick={() => navigate(routes.scheduler)}>
                  Ver agenda
                </button>
              </div>
              {publishedWeekShifts.length === 0 ? (
                <p
                  className="muted-text"
                  style={{ textAlign: "center", padding: "16px 0" }}
                >
                  Sem turnos publicados esta semana.
                </p>
              ) : (
                <div className="shift-list">
                  {publishedWeekShifts.slice(0, 5).map((shift) => {
                    const d = new Date(shift.date.slice(0, 10) + "T12:00");
                    const isToday = shift.date.slice(0, 10) === today;
                    return (
                      <div className="shift-row" key={shift.id}>
                        <div className="shift-date-badge">
                          <strong>{d.getDate()}</strong>
                          <span>{fmtDate(shift.date).slice(0, 3)}</span>
                        </div>
                        <div className="shift-row-info">
                          <div className="shift-type">
                            {shift.shiftType?.name ?? "Sem posição"}
                          </div>
                          <div className="shift-time">
                            {resolvedStartTime(shift).slice(0, 5)} —{" "}
                            {resolvedEndTime(shift).slice(0, 5)}
                          </div>
                        </div>
                        {isToday && <span className="today-tag">Hoje</span>}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Sent requests */}
            <div className="panel-card">
              <div className="panel-card-header">
                <h3>Pedidos enviados</h3>
                <button onClick={() => navigate(routes.notifications)}>
                  Ver todos
                </button>
              </div>
              {myTimeOffs.length === 0 && mySwaps.length === 0 ? (
                <p
                  className="muted-text"
                  style={{ textAlign: "center", padding: "16px 0" }}
                >
                  Sem pedidos enviados.
                </p>
              ) : (
                <div className="request-list">
                  {myTimeOffs.slice(0, 3).map((r) => (
                    <div className="request-row" key={`tor-${r.id}`}>
                      <span className="request-type-pill ferias">Férias</span>
                      <div className="request-row-info">
                        <strong>
                          {r.startDate.slice(0, 10)} — {r.endDate.slice(0, 10)}
                        </strong>
                        <span>{r.reason ?? "Sem motivo"}</span>
                      </div>
                      <span
                        className={`status-badge ${r.status.toLowerCase()}`}
                      >
                        {statusLabel(r.status)}
                      </span>
                    </div>
                  ))}
                  {mySwaps.slice(0, 3).map((r) => (
                    <div className="request-row" key={`ssr-${r.id}`}>
                      <span className="request-type-pill troca">Troca</span>
                      <div className="request-row-info">
                        <strong>Pedido de troca de turno</strong>
                        <span>{r.requesterShift?.user?.name ?? "—"}</span>
                      </div>
                      <span
                        className={`status-badge ${r.status.toLowerCase()}`}
                      >
                        {statusLabel(r.status)}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Clock-in card */}
          <div className="panel-card">
            <div className="panel-card-header">
              <h3>Registo de ponto</h3>
            </div>

            {todayShift ? (
              canRegisterPoint ? (
                <p className="muted-text">
                  Turno de hoje:{" "}
                  <strong style={{ color: "var(--text)" }}>
                    {resolvedStartTime(todayShift).slice(0, 5)} —{" "}
                    {resolvedEndTime(todayShift).slice(0, 5)}
                  </strong>{" "}
                  · Para registar ponto aceda ao ecrã de{" "}
                  <button
                    style={{
                      background: "none",
                      border: "none",
                      color: "var(--accent)",
                      cursor: "pointer",
                      fontSize: 13,
                    }}
                    onClick={() => navigate(routes.checkIn)}
                  >
                    Check-in
                  </button>
                  .
                </p>
              ) : (
                <p className="muted-text">
                  Turno de hoje:{" "}
                  <strong style={{ color: "var(--text)" }}>
                    {resolvedStartTime(todayShift).slice(0, 5)} —{" "}
                    {resolvedEndTime(todayShift).slice(0, 5)}
                  </strong>{" "}
                  · O registo de ponto só fica disponível durante o turno.
                </p>
              )
            ) : (
              <p className="muted-text">Sem turno agendado para hoje.</p>
            )}
          </div>
        </>
      )}
    </>
  );
}
