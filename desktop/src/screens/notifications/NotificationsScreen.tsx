import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  getShiftSwapRequests,
  getTimeOffRequests,
  respondToSwapRequest,
  updateShiftSwapRequestStatus,
  updateTimeOffRequestStatus,
} from "../../api/requestApi";
import type { ShiftSwapRequest, TimeOffRequest } from "../../models/request";
import { routes } from "../../navigation/routes";
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
  if (status === "PENDING") return "Pendente";
  if (status === "APPROVED") return "Aprovado";
  if (status === "REJECTED") return "Rejeitado";
  return status;
}

function formatDate(date: string): string {
  try {
    return new Intl.DateTimeFormat("pt-PT", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    }).format(new Date(date));
  } catch {
    return date.slice(0, 10);
  }
}

function formatDateTime(date: string): string {
  try {
    return new Intl.DateTimeFormat("pt-PT", {
      day: "2-digit",
      month: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    }).format(new Date(date));
  } catch {
    return date;
  }
}

export function NotificationsScreen() {
  const navigate = useNavigate();

  const [timeOffRequests, setTimeOffRequests] = useState<TimeOffRequest[]>([]);
  const [swapRequests, setSwapRequests] = useState<ShiftSwapRequest[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [actionId, setActionId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const currentUserId = tokenManager.getUserId();
  const role = tokenManager.getUserRole();
  const canManage = role === "ADMIN" || role === "MANAGER";

  const notifications = useMemo<NotificationItem[]>(() => {
    const timeOffItems: NotificationItem[] = timeOffRequests.map((request) => ({
      kind: "timeOff",
      id: request.id,
      title: "Pedido de férias",
      description: `${formatDate(request.startDate)} até ${formatDate(request.endDate)}${
        request.reason ? ` · ${request.reason}` : ""
      }`,
      status: request.status,
      createdAt: request.createdAt,
      requesterName: request.user?.name ?? `Utilizador #${request.userId}`,
      isOwnRequest: request.userId === currentUserId,
      isTargetResponse: false,
    }));

    const swapItems: NotificationItem[] = swapRequests.map((request) => {
      const requesterShift = request.requesterShift;
      const targetShift = request.targetShift;

      const requesterDate = requesterShift
        ? formatDate(requesterShift.date)
        : "Turno original";

      const targetDate = targetShift
        ? formatDate(targetShift.date)
        : "Turno alvo";

      const requesterTime = requesterShift
        ? `${requesterShift.startTime ?? requesterShift.shiftType?.startTime ?? "??:??"}-${requesterShift.endTime ?? requesterShift.shiftType?.endTime ?? "??:??"}`
        : "";

      const targetTime = targetShift
        ? `${targetShift.startTime ?? targetShift.shiftType?.startTime ?? "??:??"}-${targetShift.endTime ?? targetShift.shiftType?.endTime ?? "??:??"}`
        : "";

      const isTargetResponse =
        targetShift?.user?.id === currentUserId &&
        request.targetAccepted === null &&
        request.status === "PENDING";

      return {
        kind: "swap",
        id: request.id,
        title: "Pedido de troca de turno",
        description: `${requesterDate} ${requesterTime} ↔ ${targetDate} ${targetTime}${
          request.reason ? ` · ${request.reason}` : ""
        }`,
        status: request.status,
        createdAt: request.createdAt,
        requesterName:
          request.requester?.name ?? `Utilizador #${request.requesterId}`,
        isOwnRequest: request.requesterId === currentUserId,
        isTargetResponse,
        targetAccepted: request.targetAccepted,
      };
    });

    let allItems = [...timeOffItems, ...swapItems];

    if (canManage) {
      allItems = allItems.filter((item) => {
        if (item.kind === "timeOff") {
          return true;
        }

        return (
          item.targetAccepted === true ||
          item.status !== "PENDING" ||
          item.isOwnRequest
        );
      });
    } else {
      allItems = allItems.filter((item) => {
        if (item.kind === "timeOff") {
          return item.isOwnRequest;
        }

        return item.isOwnRequest || item.isTargetResponse;
      });
    }

    return allItems.sort((a, b) => {
      return b.createdAt.localeCompare(a.createdAt);
    });
  }, [timeOffRequests, swapRequests, currentUserId, canManage]);
  async function loadNotifications() {
    try {
      setIsLoading(true);
      setError(null);

      const [timeOff, swaps] = await Promise.allSettled([
        getTimeOffRequests(),
        getShiftSwapRequests(),
      ]);

      setTimeOffRequests(timeOff.status === "fulfilled" ? timeOff.value : []);
      setSwapRequests(swaps.status === "fulfilled" ? swaps.value : []);
    } catch {
      setError("Erro ao carregar notificações");
    } finally {
      setIsLoading(false);
    }
  }

  async function updateStatus(
    item: NotificationItem,
    status: "APPROVED" | "REJECTED",
  ) {
    try {
      const key = `${item.kind}-${item.id}`;
      setActionId(key);
      setError(null);

      if (item.kind === "timeOff") {
        const updated = await updateTimeOffRequestStatus(item.id, { status });

        setTimeOffRequests((current) => {
          return current.map((request) =>
            request.id === updated.id ? updated : request,
          );
        });
      } else {
        const updated = await updateShiftSwapRequestStatus(item.id, { status });

        setSwapRequests((current) => {
          return current.map((request) =>
            request.id === updated.id ? updated : request,
          );
        });
      }
    } catch (error) {
      setError(getBackendErrorMessage(error, "Erro ao atualizar pedido"));
    } finally {
      setActionId(null);
    }
  }

  useEffect(() => {
    void loadNotifications();
  }, []);

  return (
    <main className="notifications-page">
      <section className="notifications-shell">
        <header className="screen-header dark-header">
          <button
            className="secondary-button"
            onClick={() => navigate(routes.dashboard)}
          >
            Voltar
          </button>

          <div>
            <h1>Notificações</h1>
            <p>Pedidos e atualizações recentes</p>
          </div>
        </header>

        <section className="panel-card">
          <div className="panel-header">
            <h2>Recentes</h2>
            <button onClick={loadNotifications} disabled={isLoading}>
              {isLoading ? "A carregar..." : "Atualizar"}
            </button>
          </div>

          {error && <div className="dashboard-error">{error}</div>}

          {notifications.length === 0 ? (
            <p className="muted-text">Sem notificações.</p>
          ) : (
            <div className="notification-list">
              {notifications.map((item) => {
                const key = `${item.kind}-${item.id}`;
                const isPending = item.status === "PENDING";

                return (
                  <article className="notification-row expanded" key={key}>
                    <div>
                      <strong>{item.title}</strong>
                      <p>{item.description}</p>
                      <small>
                        {item.requesterName} · {formatDateTime(item.createdAt)}
                      </small>
                    </div>

                    <div className="notification-actions">
                      <span
                        className={`status-pill status-${item.status.toLowerCase()}`}
                      >
                        {statusLabel(item.status)}
                      </span>

                      {item.kind === "swap" && item.isTargetResponse && (
                        <div className="inline-actions">
                          <button
                            className="success-mini-button"
                            onClick={() => respondSwap(item, true)}
                            disabled={actionId === key}
                          >
                            Aceitar troca
                          </button>

                          <button
                            className="reject-mini-button"
                            onClick={() => respondSwap(item, false)}
                            disabled={actionId === key}
                          >
                            Recusar troca
                          </button>
                        </div>
                      )}

                      {canManage &&
                        isPending &&
                        !item.isOwnRequest &&
                        !item.isTargetResponse && (
                          <div className="inline-actions">
                            <button
                              className="success-mini-button"
                              onClick={() => updateStatus(item, "APPROVED")}
                              disabled={actionId === key}
                            >
                              Aprovar
                            </button>

                            <button
                              className="reject-mini-button"
                              onClick={() => updateStatus(item, "REJECTED")}
                              disabled={actionId === key}
                            >
                              Rejeitar
                            </button>
                          </div>
                        )}
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </section>
      </section>
    </main>
  );

  async function respondSwap(item: NotificationItem, accepted: boolean) {
    if (item.kind !== "swap") {
      return;
    }

    try {
      const key = `${item.kind}-${item.id}`;
      setActionId(key);
      setError(null);

      const updated = await respondToSwapRequest(item.id, accepted);

      setSwapRequests((current) => {
        return current.map((request) =>
          request.id === updated.id ? updated : request,
        );
      });
    } catch (error) {
      setError(
        getBackendErrorMessage(
          error,
          accepted
            ? "Erro ao aceitar pedido de troca"
            : "Erro ao recusar pedido de troca",
        ),
      );
    } finally {
      setActionId(null);
    }
  }
}
