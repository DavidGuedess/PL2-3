import { useEffect, useMemo, useState } from "react";
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, Legend,
} from "recharts";
import { ChevronLeft, ChevronRight, RefreshCw, Info } from "lucide-react";

import { getShifts, getMyShifts } from "../../api/shiftApi";
import { getAttendance, getMyAttendanceHistory } from "../../api/attendanceApi";
import { getUsers } from "../../api/userApi";
import { resolvedStartTime, resolvedEndTime } from "../../models/shift";
import type { Shift } from "../../models/shift";
import type { AttendanceRecord } from "../../models/attendance";
import type { User } from "../../models/user";
import { tokenManager } from "../../storage/tokenManager";

// ── date helpers ─────────────────────────────────────────────────────────────
function monday(date: Date): Date {
  const d = new Date(date);
  const day = d.getDay();
  d.setDate(d.getDate() + (day === 0 ? -6 : 1 - day));
  d.setHours(0, 0, 0, 0);
  return d;
}
function iso(d: Date) { return d.toISOString().slice(0, 10); }
function addDays(d: Date, n: number) { const c = new Date(d); c.setDate(c.getDate() + n); return c; }
function fmtWeek(start: Date): string {
  const end = addDays(start, 6);
  return `${iso(start)} — ${iso(end)}`;
}
function fmtDayLabel(d: Date): string {
  return new Intl.DateTimeFormat("pt-PT", { weekday: "short", day: "numeric" }).format(d);
}

// ── hour calculations ─────────────────────────────────────────────────────────
function shiftDurationH(shift: Shift): number {
  const [sh, sm] = resolvedStartTime(shift).split(":").map(Number);
  const [eh, em] = resolvedEndTime(shift).split(":").map(Number);
  let mins = (eh * 60 + em) - (sh * 60 + sm);
  if (mins < 0) mins += 24 * 60; // overnight
  return mins / 60;
}

function workedHoursForDay(records: AttendanceRecord[], date: string): number {
  const sorted = records
    .filter(r => r.timestamp.slice(0, 10) === date)
    .sort((a, b) => a.timestamp.localeCompare(b.timestamp));
  let mins = 0, lastIn: string | null = null;
  for (const r of sorted) {
    if (r.type === "IN") lastIn = r.timestamp;
    else if (r.type === "OUT" && lastIn) {
      mins += Math.max(0, (new Date(r.timestamp).getTime() - new Date(lastIn).getTime()) / 60000);
      lastIn = null;
    }
  }
  return mins / 60;
}

function fmtH(h: number): string {
  if (h === 0) return "0h";
  const hh = Math.floor(h);
  const mm = Math.round((h - hh) * 60);
  return mm > 0 ? `${hh}h ${mm}min` : `${hh}h`;
}

// ── punctuality ───────────────────────────────────────────────────────────────
function isPunctual(shift: Shift, records: AttendanceRecord[], marginMin = 15): boolean {
  const date = shift.date.slice(0, 10);
  const [sh, sm] = resolvedStartTime(shift).split(":").map(Number);
  const scheduledTs = new Date(`${date}T${String(sh).padStart(2,"0")}:${String(sm).padStart(2,"0")}:00`).getTime();
  const ins = records.filter(r => r.userId === shift.userId && r.type === "IN" && r.timestamp.slice(0, 10) === date);
  if (ins.length === 0) return false;
  const firstIn = Math.min(...ins.map(r => new Date(r.timestamp).getTime()));
  const diffMin = (firstIn - scheduledTs) / 60000;
  return diffMin <= marginMin;
}

// ─────────────────────────────────────────────────────────────────────────────
export function ReportsScreen() {
  const role      = tokenManager.getUserRole() ?? "EMPLOYEE";
  const canManage = role === "ADMIN" || role === "MANAGER";
  const myId      = tokenManager.getUserId();

  const [weekStart, setWeekStart] = useState(monday(new Date()));
  const [shifts,    setShifts]    = useState<Shift[]>([]);
  const [records,   setRecords]   = useState<AttendanceRecord[]>([]);
  const [users,     setUsers]     = useState<User[]>([]);
  const [selUserId, setSelUserId] = useState<number | "all">("all");
  const [isLoading, setIsLoading] = useState(false);

  const weekEnd = addDays(weekStart, 6);
  const weekDays = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i));

  async function load() {
    setIsLoading(true);
    try {
      const week = iso(weekStart);
      const from = iso(weekStart);
      const to   = iso(weekEnd);

      const [s, att, u] = await Promise.allSettled([
        canManage ? getShifts({ week }) : getMyShifts({ week }),
        canManage
          ? getAttendance({ from, to })
          : getMyAttendanceHistory({ from, to }),
        canManage ? getUsers() : Promise.resolve([] as User[]),
      ]);

      setShifts(s.status === "fulfilled" ? s.value : []);
      setRecords(att.status === "fulfilled" ? att.value : []);
      if (u.status === "fulfilled") setUsers(u.value.filter(x => x.active));
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => { void load(); }, [weekStart]);

  // ── filter by selected user ─────────────────────────────────────────────
  const filteredShifts = useMemo(() =>
    selUserId === "all" ? shifts : shifts.filter(s => s.userId === selUserId),
    [shifts, selUserId],
  );
  const filteredRecords = useMemo(() =>
    selUserId === "all" ? records
      : records.filter(r => r.userId === selUserId),
    [records, selUserId],
  );

  // ── bar chart data ──────────────────────────────────────────────────────
  const chartData = useMemo(() =>
    weekDays.map(day => {
      const key = iso(day);
      const dayShifts = filteredShifts.filter(s => s.date.slice(0, 10) === key && s.published);
      const programmed = dayShifts.reduce((sum, s) => sum + shiftDurationH(s), 0);
      const real       = workedHoursForDay(filteredRecords, key);
      return {
        label:      fmtDayLabel(day),
        Programadas: Math.round(programmed * 100) / 100,
        Reais:       Math.round(real * 100) / 100,
      };
    }),
    [filteredShifts, filteredRecords, weekDays],
  );

  // ── KPIs ────────────────────────────────────────────────────────────────
  const totalProgrammed = chartData.reduce((s, d) => s + d.Programadas, 0);
  const totalReal       = chartData.reduce((s, d) => s + d.Reais, 0);

  const publishedShifts = filteredShifts.filter(s => s.published);

  const punctualCount = publishedShifts.filter(s => isPunctual(s, filteredRecords)).length;
  const punctualRate  = publishedShifts.length > 0
    ? Math.round((punctualCount / publishedShifts.length) * 100)
    : null;

  const totalOvertime = Math.max(0, totalReal - totalProgrammed);

  // per-employee summary table
  const employeeSummary = useMemo(() => {
    if (!canManage || selUserId !== "all") return [];
    const byUser = new Map<number, { name: string; empNum: string; programmed: number; real: number; onTime: number; total: number }>();
    for (const s of publishedShifts) {
      if (!byUser.has(s.userId)) {
        const u = users.find(x => x.id === s.userId);
        byUser.set(s.userId, { name: u?.name ?? `#${s.userId}`, empNum: u?.employeeNumber ?? "", programmed: 0, real: 0, onTime: 0, total: 0 });
      }
      const entry = byUser.get(s.userId)!;
      entry.programmed += shiftDurationH(s);
      entry.total++;
      if (isPunctual(s, records)) entry.onTime++;
    }
    for (const r of records) {
      if (!byUser.has(r.userId)) {
        const u = users.find(x => x.id === r.userId);
        byUser.set(r.userId, { name: u?.name ?? `#${r.userId}`, empNum: u?.employeeNumber ?? "", programmed: 0, real: 0, onTime: 0, total: 0 });
      }
    }
    for (const day of weekDays) {
      const key = iso(day);
      for (const [uid, entry] of byUser) {
        entry.real += workedHoursForDay(records.filter(r => r.userId === uid), key);
      }
    }
    return Array.from(byUser.values()).sort((a, b) => a.name.localeCompare(b.name));
  }, [publishedShifts, records, users, weekDays, selUserId]);

  return (
    <div className="reports-page">
      {/* Sidebar */}
      {canManage && (
        <div className="reports-sidebar">
          <div className="reports-sidebar-head">
            <h2>Filtros</h2>
          </div>
          <div className="reports-sidebar-body">
            <div style={{ fontSize: 11, fontWeight: 700, color: "var(--text-muted)", textTransform: "uppercase", letterSpacing: "0.6px", marginBottom: 8 }}>
              Funcionário
            </div>
            <button
              className={`dir-item${selUserId === "all" ? " active" : ""}`}
              onClick={() => setSelUserId("all")}
              style={{ borderRadius: 6, marginBottom: 2 }}
            >
              <div className="user-avatar sm" style={{ background: "var(--surface-3)", color: "var(--text-muted)", fontSize: 10 }}>∑</div>
              <div><div className="dir-item-name">Todos</div></div>
            </button>
            {users.map(u => (
              <button
                key={u.id}
                className={`dir-item${selUserId === u.id ? " active" : ""}`}
                onClick={() => setSelUserId(u.id)}
                style={{ borderRadius: 6, marginBottom: 2 }}
              >
                <div className="user-avatar sm">{u.name.charAt(0)}</div>
                <div>
                  <div className="dir-item-name">{u.name}</div>
                  <div className="dir-item-sub">{u.employeeNumber}</div>
                </div>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Main */}
      <div className="reports-main">
        {/* Toolbar */}
        <div className="reports-toolbar">
          <h1>Visão Geral</h1>
          <div style={{ display: "flex", alignItems: "center", gap: 8, marginLeft: "auto" }}>
            <button className="btn btn-secondary btn-sm" onClick={() => setWeekStart(addDays(weekStart, -7))}>
              <ChevronLeft size={15} />
            </button>
            <span style={{ fontSize: 13, color: "var(--text-muted)", minWidth: 220, textAlign: "center" }}>
              {fmtWeek(weekStart)}
            </span>
            <button className="btn btn-secondary btn-sm" onClick={() => setWeekStart(addDays(weekStart, 7))}>
              <ChevronRight size={15} />
            </button>
            <button className="btn btn-ghost btn-sm" onClick={() => setWeekStart(monday(new Date()))}>Hoje</button>
            <button className="btn btn-ghost btn-sm" onClick={load} disabled={isLoading}>
              <RefreshCw size={13} className={isLoading ? "spin" : ""} />
            </button>
          </div>
        </div>

        <div className="reports-body">
          {/* ── Chart card ── */}
          <div className="reports-chart-card">
            <div className="reports-chart-area">
              <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 20 }}>
                <h2 style={{ fontSize: 15, fontWeight: 700, color: "var(--text)" }}>
                  Horas programadas vs. reais registadas
                </h2>
                <Info size={14} color="var(--text-faint)" />
              </div>

              {isLoading ? (
                <div className="loading-state" style={{ height: 220 }}>A carregar...</div>
              ) : (
                <ResponsiveContainer width="100%" height={240}>
                  <BarChart data={chartData} barCategoryGap="30%" barGap={4}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                    <XAxis
                      dataKey="label"
                      tick={{ fill: "var(--text-muted)", fontSize: 12 }}
                      axisLine={false}
                      tickLine={false}
                    />
                    <YAxis
                      tickFormatter={v => `${v}h`}
                      tick={{ fill: "var(--text-muted)", fontSize: 11 }}
                      axisLine={false}
                      tickLine={false}
                      width={40}
                    />
                    <Tooltip
                      contentStyle={{ background: "var(--surface-2)", border: "1px solid var(--border)", borderRadius: 8, fontSize: 13 }}
                      labelStyle={{ color: "var(--text)", fontWeight: 600 }}
                      formatter={(val, name) => [`${val}h`, name]}
                    />
                    <Legend
                      wrapperStyle={{ fontSize: 12, color: "var(--text-muted)", paddingTop: 12 }}
                      formatter={val => <span style={{ color: "var(--text-muted)" }}>{val}</span>}
                    />
                    <Bar dataKey="Programadas" fill="#2979ff" radius={[4, 4, 0, 0]} />
                    <Bar dataKey="Reais"       fill="#27ae60" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </div>

            {/* Summary panel */}
            <div className="reports-chart-summary">
              <div style={{ fontSize: 10, fontWeight: 700, color: "var(--text-muted)", textTransform: "uppercase", letterSpacing: "0.8px", marginBottom: 16 }}>
                Discriminação por tipo de horas
              </div>
              <div className="reports-summary-row">
                <span className="reports-summary-dot" style={{ background: "#2979ff" }} />
                <span style={{ flex: 1, fontSize: 13, color: "var(--text-muted)" }}>Horas programadas</span>
                <strong style={{ fontSize: 14, color: "var(--text)" }}>{totalProgrammed.toFixed(2)}</strong>
              </div>
              <div className="reports-summary-row">
                <span className="reports-summary-dot" style={{ background: "#27ae60" }} />
                <span style={{ flex: 1, fontSize: 13, color: "var(--text-muted)" }}>Horas reais</span>
                <strong style={{ fontSize: 14, color: "var(--text)" }}>{totalReal.toFixed(2)}</strong>
              </div>
              <div className="divider" style={{ margin: "12px 0" }} />
              <div className="reports-summary-row">
                <span style={{ flex: 1, fontSize: 12, color: "var(--text-muted)" }}>Diferença</span>
                <strong style={{ fontSize: 13, color: totalReal >= totalProgrammed ? "var(--green)" : "var(--red)" }}>
                  {totalReal >= totalProgrammed ? "+" : ""}{(totalReal - totalProgrammed).toFixed(2)}h
                </strong>
              </div>
              <div className="reports-summary-row">
                <span style={{ flex: 1, fontSize: 12, color: "var(--text-muted)" }}>Turnos publicados</span>
                <strong style={{ fontSize: 13, color: "var(--text)" }}>{publishedShifts.length}</strong>
              </div>
            </div>
          </div>

          {/* ── KPI row ── */}
          <div className="reports-kpi-row">
            <div className="reports-kpi-card">
              <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 16 }}>
                <h3 style={{ fontSize: 14, fontWeight: 700, color: "var(--text)" }}>Taxa de pontualidade</h3>
                <Info size={13} color="var(--text-faint)" />
              </div>
              {publishedShifts.length === 0 ? (
                <p className="reports-kpi-na">N/A</p>
              ) : (
                <>
                  <p className="reports-kpi-value" style={{ color: punctualRate !== null && punctualRate >= 80 ? "var(--green)" : "var(--orange)" }}>
                    {punctualRate !== null ? `${punctualRate}%` : "N/A"}
                  </p>
                  <p style={{ fontSize: 12, color: "var(--text-muted)", marginTop: 8 }}>
                    {punctualCount} de {publishedShifts.length} turnos com entrada pontual (±15 min)
                  </p>
                  {/* Mini bar */}
                  {punctualRate !== null && (
                    <div style={{ height: 6, background: "var(--surface-3)", borderRadius: 3, marginTop: 12, overflow: "hidden" }}>
                      <div style={{ width: `${punctualRate}%`, height: "100%", background: punctualRate >= 80 ? "var(--green)" : "var(--orange)", borderRadius: 3, transition: "width 0.4s" }} />
                    </div>
                  )}
                </>
              )}
            </div>

            <div className="reports-kpi-card">
              <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 16 }}>
                <h3 style={{ fontSize: 14, fontWeight: 700, color: "var(--text)" }}>Taxa de horas extras</h3>
                <Info size={13} color="var(--text-faint)" />
              </div>
              {totalProgrammed === 0 ? (
                <p className="reports-kpi-na">N/A</p>
              ) : (
                <>
                  <p className="reports-kpi-value" style={{ color: totalOvertime > 0 ? "var(--orange)" : "var(--green)" }}>
                    {fmtH(totalOvertime)}
                  </p>
                  <p style={{ fontSize: 12, color: "var(--text-muted)", marginTop: 8 }}>
                    {totalOvertime > 0
                      ? `${((totalOvertime / totalProgrammed) * 100).toFixed(1)}% acima do programado`
                      : "Dentro do horário programado"}
                  </p>
                </>
              )}
            </div>

            <div className="reports-kpi-card">
              <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 16 }}>
                <h3 style={{ fontSize: 14, fontWeight: 700, color: "var(--text)" }}>Cobertura semanal</h3>
                <Info size={13} color="var(--text-faint)" />
              </div>
              {publishedShifts.length === 0 ? (
                <p className="reports-kpi-na">N/A</p>
              ) : (
                <>
                  <p className="reports-kpi-value" style={{ color: "var(--accent)" }}>
                    {publishedShifts.length}
                  </p>
                  <p style={{ fontSize: 12, color: "var(--text-muted)", marginTop: 8 }}>
                    turnos publicados · {shifts.filter(s => !s.published).length} em rascunho
                  </p>
                </>
              )}
            </div>
          </div>

          {/* ── Employee table (admin/manager + all) ── */}
          {canManage && selUserId === "all" && employeeSummary.length > 0 && (
            <div className="reports-table-card">
              <h2 style={{ fontSize: 15, fontWeight: 700, color: "var(--text)", marginBottom: 16 }}>
                Resumo por funcionário
              </h2>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Funcionário</th>
                    <th>Turnos</th>
                    <th>H. Programadas</th>
                    <th>H. Reais</th>
                    <th>Diferença</th>
                    <th>Pontualidade</th>
                  </tr>
                </thead>
                <tbody>
                  {employeeSummary.map(row => {
                    const diff = row.real - row.programmed;
                    const pct  = row.total > 0 ? Math.round((row.onTime / row.total) * 100) : null;
                    return (
                      <tr key={row.empNum} style={{ cursor: "default" }}>
                        <td>
                          <div className="user-cell">
                            <div className="user-avatar sm">{row.name.charAt(0)}</div>
                            <div className="user-info">
                              <strong>{row.name}</strong>
                              <span>{row.empNum}</span>
                            </div>
                          </div>
                        </td>
                        <td>{row.total}</td>
                        <td>{fmtH(row.programmed)}</td>
                        <td>{fmtH(row.real)}</td>
                        <td style={{ color: diff >= 0 ? "var(--green)" : "var(--red)", fontWeight: 600 }}>
                          {diff >= 0 ? "+" : ""}{fmtH(Math.abs(diff))}
                        </td>
                        <td>
                          {pct !== null ? (
                            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                              <div style={{ flex: 1, height: 5, background: "var(--surface-3)", borderRadius: 3, overflow: "hidden" }}>
                                <div style={{ width: `${pct}%`, height: "100%", background: pct >= 80 ? "var(--green)" : "var(--orange)", borderRadius: 3 }} />
                              </div>
                              <span style={{ fontSize: 12, color: "var(--text-muted)", width: 32, textAlign: "right" }}>{pct}%</span>
                            </div>
                          ) : <span style={{ color: "var(--text-faint)" }}>—</span>}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
