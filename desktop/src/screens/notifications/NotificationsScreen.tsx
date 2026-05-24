import { useEffect, useMemo, useState } from "react";

import {
  getShiftSwapRequests,
  getTimeOffRequests,
  respondToSwapRequest,
  updateShiftSwapRequestStatus,
  updateTimeOffRequestStatus,
} from "../../api/requestApi";
import type { ShiftSwapRequest, TimeOffRequest } from "../../models/request";
import { tokenManager } from "../../storage/tokenManager";
import { getBackendErrorMessage } from "../../utils/errorUtils";

type NotificationItem =
  | {
      kind: "timeOff";
      id: number;
      title: string;
      description: string;
      status: string;
      createdAt: string;
      requesterName: string;
      isOwnRequest: boolean;
      isTargetResponse: false;
    }
  | {
      kind: "swap";
      id: number;
      title: string;
      description: string;
      status: string;
      createdAt: string;
      requesterName: string;
      isOwnRequest: boolean;
      isTargetResponse: boolean;
      targetAccepted: boolean | null;
    };

function statusLabel(status: string): string {
  if (status === "PENDING")  return "Pendente";
  if (status === "APPROVED") return "Aprovado";
  if (status === "REJECTED") return "Rejeitado";
  return status;
}

function fmtDate(date: string): string {
  try {
    return new Intl.DateTimeFormat("pt-PT", { day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(date));
  } catch { return date.slice(0, 10); }
}

function fmtDateTime(date: string): string {
  try {
    return new Intl.DateTimeFormat("pt-PT", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" }).format(new Date(date));
  } catch { return date; }
}

export function NotificationsScreen() {
  const [timeOffRequests, setTimeOffRequests] = useState<TimeOffRequest[]>([]);
  const [swapRequests,    setSwapRequests]    = useState<ShiftSwapRequest[]>([]);
  const [isLoading,       setIsLoading]       = useState(false);
  const [actionId,        setActionId]        = useState<string | null>(null);
  const [error,           setError]           = useState<string | null>(null);

  const currentUserId = tokenManager.getUserId();
  const role          = tokenManager.getUserRole();
  const canManage     = role === "ADMIN" || role === "MANAGER";

  const notifications = useMemo<NotificationItem[]>(() => {
    const timeOffItems: NotificationItem[] = timeOffRequests.map(request => ({
      kind: "timeOff",
      id: request.id,
      title: "Pedido de ferias",
      description: `${fmtDate(request.startDate)} ate ${fmtDate(request.endDate)}${request.reason ? ` · ${request.reason}` : ""}`,
      status: request.status,
      createdAt: request.createdAt,
      requesterName: request.user?.name ?? `Utilizador #${request.userId}`,
      isOwnRequest: request.userId === currentUserId,
      isTargetResponse: false,
    }));

    const swapItems: NotificationItem[] = swapRequests.map(request => {
      const requesterShift = request.requesterShift;
      const targetShift    = request.targetShift;
      const requesterDate  = requesterShift ? fmtDate(requesterShift.date) : "Turno original";
      const targetDate     = targetShift    ? fmtDate(targetShift.date)    : "Turno alvo";
      const requesterTime  = requesterShift ? `${requesterShift.startTime ?? requesterShift.shiftType?.startTime ?? "??:??"}-${requesterShift.endTime ?? requesterShift.shiftType?.endTime ?? "??:??"}` : "";
      const targetTime     = targetShift    ? `${targetShift.startTime ?? targetShift.shiftType?.startTime ?? "??:??"}-${targetShift.endTime ?? targetShift.shiftType?.endTime ?? "??:??"}` : "";
      const isTargetResponse = targetShift?.user?.id === currentUserId && request.targetAccepted === null && request.status === "PENDING";
      return {
        kind: "swap",
        id: request.id,
        title: "Pedido de troca de turno",
        description: `${requesterDate} ${requesterTime} ↔ ${targetDate} ${targetTime}${request.reason ? ` · ${request.reason}` : ""}`,
        status: request.status,
        createdAt: request.createdAt,
        requesterName: request.requester?.name ?? `Utilizador #${request.requesterId}`,
        isOwnRequest: request.requesterId === currentUserId,
        isTargetResponse,
        targetAccepted: request.targetAccepted,
      };
    });

    let all = [...timeOffItems, ...swapItems];

    if (canManage) {
      all = all.filter(item => {
        if (item.kind === "timeOff") return true;
        return item.targetAccepted === true || item.status !== "PENDING" || item.isOwnRequest;
      });
    } else {
      all = all.filter(item => {
        if (item.kind === "timeOff") return item.isOwnRequest;
        return item.isOwnRequest || item.isTargetResponse;
      });
    }

    return all.sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  }, [timeOffRequests, swapRequests, currentUserId, canManage]);

  async function loadNotifications() {
    setIsLoading(true); setError(null);
    try {
      const [timeOff, swaps] = await Promise.allSettled([getTimeOffRequests(), getShiftSwapRequests()]);
      setTimeOffRequests(timeOff.status === "fulfilled" ? timeOff.value : []);
      setSwapRequests(swaps.status === "fulfilled" ? swaps.value : []);
    } catch {
      setError("Erro ao carregar notificacoes");
    } finally {
      setIsLoading(false);
    }
  }

  async function updateStatus(item: NotificationItem, status: "APPROVED" | "REJECTED") {
    const key = `${item.kind}-${item.id}`;
    setActionId(key); setError(null);
    try {
      if (item.kind === "timeOff") {
        const updated = await updateTimeOffRequestStatus(item.id, { status });
        setTimeOffRequests(cur => cur.map(r => r.id === updated.id ? updated : r));
      } else {
        const updated = await updateShiftSwapRequestStatus(item.id, { status });
        setSwapRequests(cur => cur.map(r => r.id === updated.id ? updated : r));
      }
    } catch (e) {
      setError(getBackendErrorMessage(e, "Erro ao atualizar pedido"));
    } finally {
      setActionId(null);
    }
  }

  async function respondSwap(item: NotificationItem, accepted: boolean) {
    if (item.kind !== "swap") return;
    const key = `${item.kind}-${item.id}`;
    setActionId(key); setError(null);
    try {
      const updated = await respondToSwapRequest(item.id, accepted);
      setSwapRequests(cur => cur.map(r => r.id === updated.id ? updated : r));
    } catch (e) {
      setError(getBackendErrorMessage(e, accepted ? "Erro ao aceitar troca" : "Erro ao recusar troca"));
    } finally {
      setActionId(null);
    }
  }

  useEffect(() => { void loadNotifications(); }, []);

  const pending  = notifications.filter(n => n.status === "PENDING");
  const resolved = notifications.filter(n => n.status !== "PENDING");

  return (
    <div>
      <div className="page-header">
        <h1>Notificacoes</h1>
        <button className="btn btn-secondary btn-sm" onClick={loadNotifications} disabled={isLoading}>
          {isLoading ? "..." : "Atualizar"}
        </button>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {isLoading ? (
        <div className="loading-state">A carregar...</div>
      ) : notifications.length === 0 ? (
        <div className="panel-card">
          <p className="muted-text" style={{ textAlign: "center", padding: "24px 0" }}>Sem notificacoes.</p>
        </div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
          {pending.length > 0 && (
            <div className="panel-card">
              <div className="panel-card-header">
                <h3>Pendentes</h3>
                <span style={{ fontSize: 12, background: "rgba(230,126,34,0.15)", color: "var(--orange)", padding: "2px 8px", borderRadius: 10, fontWeight: 600 }}>
                  {pending.length}
                </span>
              </div>
              <div className="notif-list">
                {pending.map(item => <NotifRow key={`${item.kind}-${item.id}`} item={item} actionId={actionId} canManage={canManage} onApprove={() => void updateStatus(item, "APPROVED")} onReject={() => void updateStatus(item, "REJECTED")} onAcceptSwap={() => void respondSwap(item, true)} onRejectSwap={() => void respondSwap(item, false)} />)}
              </div>
            </div>
          )}

          {resolved.length > 0 && (
            <div className="panel-card">
              <div className="panel-card-header">
                <h3>Historico</h3>
              </div>
              <div className="notif-list">
                {resolved.map(item => <NotifRow key={`${item.kind}-${item.id}`} item={item} actionId={actionId} canManage={canManage} onApprove={() => void updateStatus(item, "APPROVED")} onReject={() => void updateStatus(item, "REJECTED")} onAcceptSwap={() => void respondSwap(item, true)} onRejectSwap={() => void respondSwap(item, false)} />)}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function NotifRow({ item, actionId, canManage, onApprove, onReject, onAcceptSwap, onRejectSwap }: {
  item: NotificationItem;
  actionId: string | null;
  canManage: boolean;
  onApprove: () => void;
  onReject: () => void;
  onAcceptSwap: () => void;
  onRejectSwap: () => void;
}) {
  const key      = `${item.kind}-${item.id}`;
  const isBusy   = actionId === key;
  const isPending = item.status === "PENDING";

  return (
    <div className="notif-row">
      <div className="notif-type-pill">
        <span className={`request-type-pill ${item.kind === "timeOff" ? "ferias" : "troca"}`}>
          {item.kind === "timeOff" ? "Ferias" : "Troca"}
        </span>
      </div>

      <div className="notif-body">
        <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
          <div>
            <strong style={{ fontSize: 14, color: "var(--text)" }}>{item.title}</strong>
            <div style={{ fontSize: 13, color: "var(--text-muted)", marginTop: 2 }}>{item.description}</div>
            <div style={{ fontSize: 11, color: "var(--text-faint)", marginTop: 4 }}>
              {item.requesterName} · {fmtDateTime(item.createdAt)}
              {item.isOwnRequest && <span style={{ marginLeft: 6, color: "var(--accent)" }}>(seu pedido)</span>}
            </div>
          </div>
          <span className={`status-badge ${item.status.toLowerCase()}`}>{statusLabel(item.status)}</span>
        </div>

        {isPending && (
          <div style={{ display: "flex", gap: 8, marginTop: 10, flexWrap: "wrap" }}>
            {item.kind === "swap" && item.isTargetResponse && (
              <>
                <button className="btn btn-primary btn-sm" onClick={onAcceptSwap} disabled={isBusy}>Aceitar troca</button>
                <button className="btn btn-danger btn-sm"  onClick={onRejectSwap} disabled={isBusy}>Recusar troca</button>
              </>
            )}
            {canManage && !item.isOwnRequest && !item.isTargetResponse && (
              <>
                <button className="btn btn-primary btn-sm" onClick={onApprove} disabled={isBusy}>Aprovar</button>
                <button className="btn btn-danger btn-sm"  onClick={onReject}  disabled={isBusy}>Rejeitar</button>
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
