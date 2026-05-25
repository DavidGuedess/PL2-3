import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, LogIn, LogOut, Clock } from "lucide-react";

import { getMyAttendanceHistory, registerAttendance } from "../../api/attendanceApi";
import { getMyShifts } from "../../api/shiftApi";
import type { AttendanceRecord } from "../../models/attendance";
import type { Shift } from "../../models/shift";
import { resolvedEndTime, resolvedName, resolvedStartTime } from "../../models/shift";
import { routes } from "../../navigation/routes";

function getToday(): string { return new Date().toISOString().slice(0, 10); }

function getWeekStart(date: Date): string {
  const copy = new Date(date);
  const day = copy.getDay();
  copy.setDate(copy.getDate() + (day === 0 ? -6 : 1 - day));
  return copy.toISOString().slice(0, 10);
}

function fmtDate(ts: string): string {
  try {
    return new Intl.DateTimeFormat("pt-PT", { day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(ts));
  } catch { return ts; }
}

function fmtTime(ts: string): string {
  try {
    return new Intl.DateTimeFormat("pt-PT", { hour: "2-digit", minute: "2-digit" }).format(new Date(ts));
  } catch { return ts; }
}

export function CheckInScreen() {
  const navigate = useNavigate();

  const today     = useMemo(() => getToday(), []);
  const weekStart = useMemo(() => getWeekStart(new Date()), []);

  const [isWorking, setIsWorking] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [toast,     setToast]     = useState<{ text: string; error?: boolean } | null>(null);
  const [records,   setRecords]   = useState<AttendanceRecord[]>([]);
  const [todayShift, setTodayShift] = useState<Shift | null>(null);

  // D-3: auto-clear toast after 3s
  function showToast(text: string, error = false) {
    setToast({ text, error });
    setTimeout(() => setToast(null), 3000);
  }

  async function loadTodayShift() {
    try {
      const shifts = await getMyShifts({ week: weekStart });
      setTodayShift(shifts.find(s => s.published && s.date.slice(0, 10) === today) ?? null);
    } catch {
      setTodayShift(null);
    }
  }

  async function loadHistory() {
    try {
      setIsLoading(true);
      const history = await getMyAttendanceHistory();
      const ordered = [...history].sort((a, b) => b.timestamp.localeCompare(a.timestamp));
      setRecords(ordered);
      // D-2: only TODAY's records determine working state
      const todayRecords = ordered.filter(r => r.timestamp.slice(0, 10) === today);
      setIsWorking(todayRecords.length > 0 && todayRecords[0].type === "IN");
    } catch {
      showToast("Erro ao carregar histórico.", true);
    } finally {
      setIsLoading(false);
    }
  }

  async function handleRegister(type: "IN" | "OUT") {
    try {
      setIsLoading(true);
      await registerAttendance({ type });
      showToast(type === "IN" ? "Entrada registada." : "Saída registada.");
      await loadHistory();
    } catch {
      showToast(`Erro ao registar ${type === "IN" ? "entrada" : "saída"}.`, true);
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    void loadHistory();
    void loadTodayShift();
  }, []);

  return (
    <div className="screen-body" style={{ maxWidth: 640, margin: "0 auto" }}>
      {/* Header */}
      <div className="page-header">
        <button className="btn btn-ghost btn-sm" onClick={() => navigate(routes.dashboard)}>
          <ArrowLeft size={15} /> Voltar
        </button>
        <h1>Registo de Presença</h1>
      </div>

      {/* Today shift */}
      <div className="panel-card" style={{ marginBottom: 16 }}>
        <div className="panel-card-header">
          <h3>Turno de hoje</h3>
        </div>
        {todayShift ? (
          <div className="detail-info-list">
            <div className="detail-info-row">
              <span>Data</span>
              <strong>{fmtDate(todayShift.date)}</strong>
            </div>
            <div className="detail-info-row">
              <span>Horário</span>
              <strong>{resolvedStartTime(todayShift)} — {resolvedEndTime(todayShift)}</strong>
            </div>
            <div className="detail-info-row">
              <span>Posição</span>
              <strong>{resolvedName(todayShift)}</strong>
            </div>
          </div>
        ) : (
          <p className="muted-text" style={{ padding: "12px 0" }}>Sem turno agendado para hoje.</p>
        )}
      </div>

      {/* Action buttons */}
      <div className="panel-card" style={{ marginBottom: 16 }}>
        <div style={{ display: "flex", gap: 12 }}>
          <button
            className="btn btn-primary"
            style={{ flex: 1, justifyContent: "center" }}
            onClick={() => void handleRegister("IN")}
            disabled={isWorking || isLoading}
          >
            <LogIn size={16} />
            {isLoading ? "A processar..." : "Registar Entrada"}
          </button>
          <button
            className="btn btn-secondary"
            style={{ flex: 1, justifyContent: "center" }}
            onClick={() => void handleRegister("OUT")}
            disabled={!isWorking || isLoading}
          >
            <LogOut size={16} />
            {isLoading ? "A processar..." : "Registar Saída"}
          </button>
        </div>
        {isWorking && (
          <p style={{ marginTop: 10, fontSize: 12, color: "var(--green)", display: "flex", alignItems: "center", gap: 5 }}>
            <Clock size={12} /> Em turno agora
          </p>
        )}
      </div>

      {/* Recent history */}
      <div className="panel-card">
        <div className="panel-card-header">
          <h3>Histórico recente</h3>
        </div>
        {records.length === 0 ? (
          <p className="muted-text" style={{ padding: "12px 0" }}>Sem registos de presença.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Data</th>
                <th>Tipo</th>
                <th>Hora</th>
              </tr>
            </thead>
            <tbody>
              {records.slice(0, 10).map(r => (
                <tr key={r.id} style={{ cursor: "default" }}>
                  <td>{fmtDate(r.timestamp)}</td>
                  <td>
                    <span className={`status-badge ${r.type === "IN" ? "active" : "inactive"}`}>
                      {r.type === "IN" ? "Entrada" : "Saída"}
                    </span>
                  </td>
                  <td style={{ color: "var(--text-muted)" }}>{fmtTime(r.timestamp)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Toast */}
      {toast && (
        <div className="toast-container">
          <div className={`toast${toast.error ? " error" : ""}`}>
            <Clock size={14} /> {toast.text}
          </div>
        </div>
      )}
    </div>
  );
}
