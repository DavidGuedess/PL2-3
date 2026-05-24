import { useEffect, useMemo, useState } from "react";
import { Clock, FileText, Activity, ChevronRight } from "lucide-react";

import { getAttendance, getMyAttendanceHistory } from "../../api/attendanceApi";
import { getTimeOffRequests, getShiftSwapRequests } from "../../api/requestApi";
import { getActivity } from "../../api/activityApi";
import { getUsers } from "../../api/userApi";
import type { AttendanceRecord } from "../../models/attendance";
import type { TimeOffRequest, ShiftSwapRequest } from "../../models/request";
import type { ActivityItem } from "../../api/activityApi";
import type { User } from "../../models/user";
import { tokenManager } from "../../storage/tokenManager";

type Tab = "attendance" | "requests" | "activity";

function getToday() { return new Date().toISOString().slice(0, 10); }
function firstOfMonth() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-01`;
}

function fmtDate(ts: string) {
  return new Intl.DateTimeFormat("pt-PT", { weekday: "short", day: "2-digit", month: "short" }).format(new Date(ts.slice(0, 10) + "T12:00"));
}
function fmtTime(ts: string) {
  return new Intl.DateTimeFormat("pt-PT", { hour: "2-digit", minute: "2-digit" }).format(new Date(ts));
}
function fmtDateTime(ts: string) {
  return new Intl.DateTimeFormat("pt-PT", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" }).format(new Date(ts));
}

function calcTotal(records: AttendanceRecord[], date: string): string {
  const sorted = records.filter(r => r.timestamp.slice(0, 10) === date).sort((a, b) => a.timestamp.localeCompare(b.timestamp));
  let mins = 0, lastIn: string | null = null;
  for (const r of sorted) {
    if (r.type === "IN") lastIn = r.timestamp;
    else if (r.type === "OUT" && lastIn) { mins += Math.max(0, Math.floor((new Date(r.timestamp).getTime() - new Date(lastIn).getTime()) / 60000)); lastIn = null; }
  }
  const h = Math.floor(mins / 60), m = mins % 60;
  return (h === 0 && m === 0) ? "—" : `${h}h ${m}min`;
}

function dateRange(from: string, to: string): string[] {
  const dates: string[] = [];
  const cur = new Date(from);
  while (cur <= new Date(to)) { dates.push(cur.toISOString().slice(0, 10)); cur.setDate(cur.getDate() + 1); }
  return dates;
}

function initials(name: string) {
  return name.split(" ").filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join("") || "?";
}

function statusBadge(status: string) {
  const cls = status === "APPROVED" ? "approved" : status === "REJECTED" ? "rejected" : "pending";
  const label = status === "APPROVED" ? "Aprovado" : status === "REJECTED" ? "Rejeitado" : "Pendente";
  return <span className={`status-badge ${cls}`}>{label}</span>;
}

export function HistoryScreen() {
  const role      = tokenManager.getUserRole() ?? "EMPLOYEE";
  const canSeeAll = role === "ADMIN" || role === "MANAGER";
  const myId      = tokenManager.getUserId();

  const [activeTab,   setActiveTab]   = useState<Tab>("attendance");
  const [users,       setUsers]       = useState<User[]>([]);
  const [selectedId,  setSelectedId]  = useState<number | null>(null);
  const [dirSearch,   setDirSearch]   = useState("");
  const [fromDate,    setFromDate]    = useState(firstOfMonth());
  const [toDate,      setToDate]      = useState(getToday());
  const [isLoading,   setIsLoading]   = useState(false);

  // Tab 1 - Attendance
  const [records,     setRecords]     = useState<AttendanceRecord[]>([]);

  // Tab 2 - Requests
  const [timeOffs,    setTimeOffs]    = useState<TimeOffRequest[]>([]);
  const [swaps,       setSwaps]       = useState<ShiftSwapRequest[]>([]);
  const [reqFilter,   setReqFilter]   = useState<"all" | "timeoff" | "swap" | "pending" | "approved" | "rejected">("all");

  // Tab 3 - Activity
  const [activity,    setActivity]    = useState<ActivityItem[]>([]);
  const [actFilter,   setActFilter]   = useState<"all" | "attendance" | "timeoff" | "swap">("all");

  // Load users directory
  useEffect(() => {
    if (!canSeeAll) { setSelectedId(myId); return; }
    getUsers().then(list => {
      setUsers(list.filter(u => u.active));
      const me = list.find(u => u.id === myId);
      if (me) setSelectedId(me.id);
    }).catch(() => {});
  }, []);

  // Load data when tab, selectedId, or date range changes
  useEffect(() => {
    if (!selectedId) return;
    setIsLoading(true);

    const userId = canSeeAll ? selectedId : undefined;

    const load = async () => {
      try {
        if (activeTab === "attendance") {
          const data = canSeeAll
            ? await getAttendance({ userId: selectedId, from: fromDate, to: toDate })
            : await getMyAttendanceHistory({ from: fromDate, to: toDate });
          setRecords(data);
        } else if (activeTab === "requests") {
          const [tor, ssr] = await Promise.allSettled([
            getTimeOffRequests({ userId, from: fromDate, to: toDate }),
            getShiftSwapRequests({ userId, from: fromDate, to: toDate }),
          ]);
          setTimeOffs(tor.status === "fulfilled" ? tor.value : []);
          setSwaps(ssr.status === "fulfilled" ? ssr.value : []);
        } else {
          const data = await getActivity({ userId, from: fromDate, to: toDate, type: actFilter });
          setActivity(data);
        }
      } catch {
        // leave existing data
      } finally {
        setIsLoading(false);
      }
    };

    void load();
  }, [activeTab, selectedId, fromDate, toDate, actFilter]);

  const filteredUsers = useMemo(() => {
    const q = dirSearch.toLowerCase();
    return users.filter(u => !q || u.name.toLowerCase().includes(q) || u.employeeNumber.includes(q));
  }, [users, dirSearch]);

  const userGroups = useMemo(() => {
    const groups = new Map<string, User[]>();
    for (const u of filteredUsers) {
      const letter = u.name.charAt(0).toUpperCase();
      groups.set(letter, [...(groups.get(letter) ?? []), u]);
    }
    return Array.from(groups.entries()).sort(([a], [b]) => a.localeCompare(b));
  }, [filteredUsers]);

  const selectedUser = canSeeAll
    ? users.find(u => u.id === selectedId) ?? null
    : { id: myId, name: tokenManager.getUserName() ?? "Eu" } as User;

  // ── attendance tab ────────────────────────────────────────────────────────
  const dates = dateRange(fromDate, toDate).reverse();
  const recordsByDate = useMemo(() => {
    const map = new Map<string, AttendanceRecord[]>();
    for (const r of records) { const d = r.timestamp.slice(0, 10); map.set(d, [...(map.get(d) ?? []), r]); }
    return map;
  }, [records]);
  const totalWorked = useMemo(() => {
    let mins = 0;
    for (const [, recs] of recordsByDate) {
      const sorted = [...recs].sort((a, b) => a.timestamp.localeCompare(b.timestamp));
      let lastIn: string | null = null;
      for (const r of sorted) {
        if (r.type === "IN") lastIn = r.timestamp;
        else if (r.type === "OUT" && lastIn) { mins += Math.max(0, Math.floor((new Date(r.timestamp).getTime() - new Date(lastIn).getTime()) / 60000)); lastIn = null; }
      }
    }
    const h = Math.floor(mins / 60), m = mins % 60;
    return `${h}h ${m}min`;
  }, [recordsByDate]);

  // ── requests tab ──────────────────────────────────────────────────────────
  const filteredRequests = useMemo(() => {
    const combined: Array<{ kind: "timeoff" | "swap"; item: TimeOffRequest | ShiftSwapRequest; ts: string; status: string }> = [
      ...timeOffs.map(r => ({ kind: "timeoff" as const, item: r, ts: r.updatedAt ?? r.createdAt, status: r.status })),
      ...swaps.map(r => ({ kind: "swap" as const, item: r, ts: r.updatedAt ?? r.createdAt, status: r.status })),
    ].sort((a, b) => b.ts.localeCompare(a.ts));

    if (reqFilter === "all") return combined;
    if (reqFilter === "timeoff") return combined.filter(x => x.kind === "timeoff");
    if (reqFilter === "swap")    return combined.filter(x => x.kind === "swap");
    return combined.filter(x => x.status === reqFilter.toUpperCase());
  }, [timeOffs, swaps, reqFilter]);

  const TABS: { id: Tab; label: string; Icon: typeof Clock }[] = [
    { id: "attendance", label: "Registos de Ponto", Icon: Clock },
    { id: "requests",   label: "Pedidos",           Icon: FileText },
    { id: "activity",   label: "Atividade",          Icon: Activity },
  ];

  return (
    <div className="history-page">
      {/* Directory sidebar */}
      {canSeeAll && (
        <div className="history-dir">
          <div className="history-dir-head">
            <h2>Diretório</h2>
            <p>Selecionar funcionário</p>
          </div>
          <div className="history-dir-search">
            <input placeholder="Pesquisar..." value={dirSearch} onChange={e => setDirSearch(e.target.value)} />
          </div>
          <div className="history-dir-list">
            {userGroups.map(([letter, group]) => (
              <div key={letter}>
                <div className="dir-letter">{letter}</div>
                {group.map(u => (
                  <button key={u.id} className={`dir-item${u.id === selectedId ? " active" : ""}`} onClick={() => setSelectedId(u.id)}>
                    <div className="user-avatar sm">{initials(u.name)}</div>
                    <div><div className="dir-item-name">{u.name}</div><div className="dir-item-sub">{u.employeeNumber}</div></div>
                  </button>
                ))}
              </div>
            ))}
            {filteredUsers.length === 0 && <div className="history-empty">Nenhum resultado.</div>}
          </div>
        </div>
      )}

      {/* Main content */}
      <div className="history-main">
        {/* Top bar */}
        <div className="history-main-toolbar">
          <div className="history-employee-head">
            {selectedUser && (
              <>
                <div className="user-avatar">{initials(selectedUser.name)}</div>
                <h2>{selectedUser.name}</h2>
              </>
            )}
          </div>
          <div className="date-range">
            <input type="date" value={fromDate} onChange={e => setFromDate(e.target.value)} />
            <span>—</span>
            <input type="date" value={toDate} onChange={e => setToDate(e.target.value)} />
          </div>
        </div>

        {/* Tabs */}
        <div style={{ display: "flex", borderBottom: "1px solid var(--border)", background: "var(--surface)", flexShrink: 0 }}>
          {TABS.map(({ id, label, Icon }) => (
            <button
              key={id}
              onClick={() => setActiveTab(id)}
              style={{
                display: "flex", alignItems: "center", gap: 7,
                padding: "11px 20px", border: "none", borderBottom: `2px solid ${activeTab === id ? "var(--accent)" : "transparent"}`,
                background: "none", color: activeTab === id ? "var(--accent)" : "var(--text-muted)",
                cursor: "pointer", fontSize: 13, fontWeight: 500, transition: "color 0.12s",
                marginBottom: -1,
              }}
            >
              <Icon size={15} />
              {label}
            </button>
          ))}
        </div>

        {(!selectedUser && canSeeAll) ? (
          <div className="history-empty">Seleciona um funcionário para ver o histórico.</div>
        ) : isLoading ? (
          <div className="loading-state">A carregar...</div>
        ) : (
          <>
            {/* ── TAB 1: Registos de Ponto ─────────────────────────────── */}
            {activeTab === "attendance" && (
              <div className="history-table-wrap">
                <table className="history-table">
                  <thead>
                    <tr>
                      <th>Data</th><th>Total</th><th>Entrada</th><th>Saída</th><th>Registos</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dates.map(date => {
                      const dayRecords = (recordsByDate.get(date) ?? []).sort((a, b) => a.timestamp.localeCompare(b.timestamp));
                      const inRecord  = dayRecords.find(r => r.type === "IN");
                      const outRecord = [...dayRecords].reverse().find(r => r.type === "OUT");
                      const total     = calcTotal(dayRecords, date);
                      const hasData   = dayRecords.length > 0;
                      return (
                        <tr key={date}>
                          <td>{fmtDate(date)}</td>
                          <td style={{ fontWeight: hasData ? 600 : 400 }}>
                            {hasData ? total : <span style={{ color: "var(--text-faint)" }}>—</span>}
                          </td>
                          <td>{inRecord  ? <span className="attendance-in">{fmtTime(inRecord.timestamp)}</span>  : <span style={{ color: "var(--text-faint)" }}>—</span>}</td>
                          <td>{outRecord ? <span className="attendance-out">{fmtTime(outRecord.timestamp)}</span> : <span style={{ color: "var(--text-faint)" }}>—</span>}</td>
                          <td>
                            <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                              {dayRecords.map(r => (
                                <span key={r.id} style={{ fontSize: 11, padding: "2px 6px", borderRadius: 4, background: r.type === "IN" ? "rgba(39,174,96,0.15)" : "rgba(128,128,160,0.15)", color: r.type === "IN" ? "var(--green)" : "var(--text-muted)" }}>
                                  {r.type} {fmtTime(r.timestamp)}
                                </span>
                              ))}
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
                {dates.length > 0 && (
                  <div style={{ padding: "10px 20px", borderTop: "1px solid var(--border)", fontSize: 13, color: "var(--text-muted)", background: "var(--surface)" }}>
                    {dates.length} dias · Total trabalhado: <strong style={{ color: "var(--text)" }}>{totalWorked}</strong>
                  </div>
                )}
              </div>
            )}

            {/* ── TAB 2: Pedidos ───────────────────────────────────────── */}
            {activeTab === "requests" && (
              <div className="history-table-wrap">
                {/* Sub-filters */}
                <div style={{ padding: "10px 20px", borderBottom: "1px solid var(--border)", display: "flex", gap: 8, flexWrap: "wrap", background: "var(--surface)" }}>
                  {(["all", "timeoff", "swap", "pending", "approved", "rejected"] as const).map(f => (
                    <button
                      key={f}
                      onClick={() => setReqFilter(f)}
                      style={{
                        padding: "4px 12px", borderRadius: 20, border: "1px solid var(--border)", cursor: "pointer", fontSize: 12,
                        background: reqFilter === f ? "var(--accent)" : "var(--surface-2)",
                        color: reqFilter === f ? "white" : "var(--text-muted)",
                        fontWeight: reqFilter === f ? 600 : 400,
                      }}
                    >
                      {{ all: "Todos", timeoff: "Férias", swap: "Trocas", pending: "Pendentes", approved: "Aprovados", rejected: "Rejeitados" }[f]}
                    </button>
                  ))}
                </div>

                {filteredRequests.length === 0 ? (
                  <div className="history-empty">Sem pedidos no período selecionado.</div>
                ) : (
                  <table className="history-table">
                    <thead>
                      <tr><th>Tipo</th><th>Descrição</th><th>Data</th><th>Estado</th>{canSeeAll && <th>Funcionário</th>}</tr>
                    </thead>
                    <tbody>
                      {filteredRequests.map(({ kind, item, ts }) => {
                        if (kind === "timeoff") {
                          const r = item as TimeOffRequest;
                          return (
                            <tr key={`tor-${r.id}`}>
                              <td><span className="request-type-pill ferias">Férias</span></td>
                              <td>{r.startDate.slice(0, 10)} — {r.endDate.slice(0, 10)}{r.reason ? ` · ${r.reason}` : ""}</td>
                              <td style={{ color: "var(--text-muted)", fontSize: 12 }}>{fmtDateTime(ts)}</td>
                              <td>{statusBadge(r.status)}</td>
                              {canSeeAll && <td style={{ fontSize: 12 }}>{r.user?.name ?? "—"}</td>}
                            </tr>
                          );
                        }
                        const r = item as ShiftSwapRequest;
                        const reqDate = r.requesterShift?.date?.slice(0, 10) ?? "—";
                        const tarDate = r.targetShift?.date?.slice(0, 10) ?? "—";
                        return (
                          <tr key={`ssr-${r.id}`}>
                            <td><span className="request-type-pill troca">Troca</span></td>
                            <td>
                              {reqDate} <ChevronRight size={12} style={{ verticalAlign: "middle", color: "var(--text-faint)" }} /> {tarDate}
                              {r.reason ? ` · ${r.reason}` : ""}
                            </td>
                            <td style={{ color: "var(--text-muted)", fontSize: 12 }}>{fmtDateTime(ts)}</td>
                            <td>{statusBadge(r.status)}</td>
                            {canSeeAll && <td style={{ fontSize: 12 }}>{r.requester?.name ?? "—"}</td>}
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                )}
              </div>
            )}

            {/* ── TAB 3: Atividade ─────────────────────────────────────── */}
            {activeTab === "activity" && (
              <div className="history-table-wrap">
                {/* Type filter */}
                <div style={{ padding: "10px 20px", borderBottom: "1px solid var(--border)", display: "flex", gap: 8, flexWrap: "wrap", background: "var(--surface)" }}>
                  {(["all", "attendance", "timeoff", "swap"] as const).map(f => (
                    <button
                      key={f}
                      onClick={() => setActFilter(f)}
                      style={{
                        padding: "4px 12px", borderRadius: 20, border: "1px solid var(--border)", cursor: "pointer", fontSize: 12,
                        background: actFilter === f ? "var(--accent)" : "var(--surface-2)",
                        color: actFilter === f ? "white" : "var(--text-muted)",
                        fontWeight: actFilter === f ? 600 : 400,
                      }}
                    >
                      {{ all: "Todas", attendance: "Ponto", timeoff: "Férias", swap: "Trocas" }[f]}
                    </button>
                  ))}
                </div>

                {activity.length === 0 ? (
                  <div className="history-empty">Sem atividade no período selecionado.</div>
                ) : (
                  <table className="history-table">
                    <thead>
                      <tr><th>Hora</th><th>Tipo</th><th>Ação</th><th>Detalhe</th>{canSeeAll && <th>Funcionário</th>}<th>Estado</th></tr>
                    </thead>
                    <tbody>
                      {activity.map(item => (
                        <tr key={item.id}>
                          <td style={{ fontSize: 12, color: "var(--text-muted)", whiteSpace: "nowrap" }}>{fmtDateTime(item.timestamp)}</td>
                          <td>
                            <span className={`request-type-pill ${item.type === "attendance" ? "ferias" : item.type === "timeoff" ? "ferias" : "troca"}`}
                              style={item.type === "attendance" ? { background: "rgba(79,110,247,0.15)", color: "var(--accent)" } : {}}>
                              {{ attendance: "Ponto", timeoff: "Férias", swap: "Troca" }[item.type]}
                            </span>
                          </td>
                          <td style={{ fontWeight: 500 }}>{item.action}</td>
                          <td style={{ fontSize: 13, color: "var(--text-muted)", maxWidth: 260, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{item.detail}</td>
                          {canSeeAll && <td style={{ fontSize: 12 }}>{item.userName}</td>}
                          <td>{item.status ? statusBadge(item.status) : <span style={{ color: "var(--text-faint)", fontSize: 12 }}>—</span>}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
                {activity.length > 0 && (
                  <div style={{ padding: "10px 20px", borderTop: "1px solid var(--border)", fontSize: 13, color: "var(--text-muted)", background: "var(--surface)" }}>
                    {activity.length} evento(s)
                  </div>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
