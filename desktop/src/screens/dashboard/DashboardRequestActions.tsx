import { FormEvent, useEffect, useMemo, useState } from "react";

import {
  createShiftSwapRequest,
  createTimeOffRequest,
} from "../../api/requestApi";
import { getShifts } from "../../api/shiftApi";

import type { Shift } from "../../models/shift";
import {
  resolvedEndTime,
  resolvedName,
  resolvedStartTime,
} from "../../models/shift";
import { getBackendErrorMessage } from "../../utils/errorUtils";

type RequestMode = "none" | "timeOff" | "swap";

interface DashboardRequestActionsProps {
  myShifts: Shift[];
  currentUserId: number;
  weekStart: string;
  onRequestCreated: () => void;
}

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
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

function shiftLabel(shift: Shift): string {
  return `${resolvedName(shift)} — ${formatDate(shift.date)} ${resolvedStartTime(
    shift,
  ).slice(0, 5)}-${resolvedEndTime(shift).slice(0, 5)}`;
}

export function DashboardRequestActions({
  myShifts,
  currentUserId,
  weekStart,
  onRequestCreated,
}: DashboardRequestActionsProps) {
  const [mode, setMode] = useState<RequestMode>("none");

  const [timeOffStart, setTimeOffStart] = useState(todayIso());
  const [timeOffEnd, setTimeOffEnd] = useState(todayIso());
  const [timeOffAllDay, setTimeOffAllDay] = useState(true);
  const [timeOffReason, setTimeOffReason] = useState("");

  const [colleagueShifts, setColleagueShifts] = useState<Shift[]>([]);
  const [selectedMyShiftId, setSelectedMyShiftId] = useState<number | "">("");
  const [selectedColleagueShiftId, setSelectedColleagueShiftId] = useState<
    number | ""
  >("");
  const [swapReason, setSwapReason] = useState("");

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoadingColleagueShifts, setIsLoadingColleagueShifts] =
    useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const publishedMyShifts = useMemo(() => {
    return myShifts
      .filter((shift) => shift.published)
      .sort((a, b) => {
        return `${a.date}${resolvedStartTime(a)}`.localeCompare(
          `${b.date}${resolvedStartTime(b)}`,
        );
      });
  }, [myShifts]);

  async function loadColleagueShifts() {
    try {
      setIsLoadingColleagueShifts(true);

      const result = await getShifts({
        week: weekStart,
      });

      setColleagueShifts(
        result
          .filter(
            (shift) => shift.published && shift.user?.id !== currentUserId,
          )
          .sort((a, b) => {
            return `${a.date}${resolvedStartTime(a)}`.localeCompare(
              `${b.date}${resolvedStartTime(b)}`,
            );
          }),
      );
    } catch (error) {
      setMessage(
        getBackendErrorMessage(error, "Erro ao carregar turnos dos colegas."),
      );
    } finally {
      setIsLoadingColleagueShifts(false);
    }
  }

  async function submitTimeOff(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const fixedEndDate = timeOffEnd < timeOffStart ? timeOffStart : timeOffEnd;

    try {
      setIsSubmitting(true);
      setMessage(null);

      await createTimeOffRequest({
        startDate: timeOffStart,
        endDate: fixedEndDate,
        allDay: timeOffAllDay,
        reason: timeOffReason.trim() || null,
      });

      setMessage("Pedido de férias enviado com sucesso.");
      setTimeOffReason("");
      setMode("none");
      onRequestCreated();
    } catch (error) {
      setMessage(
        getBackendErrorMessage(error, "Erro ao enviar pedido de férias."),
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  async function submitSwap(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (selectedMyShiftId === "" || selectedColleagueShiftId === "") {
      setMessage("Seleciona o teu turno e o turno do colega.");
      return;
    }

    try {
      setIsSubmitting(true);
      setMessage(null);

      await createShiftSwapRequest({
        requesterShiftId: Number(selectedMyShiftId),
        targetShiftId: Number(selectedColleagueShiftId),
        reason: swapReason.trim() || null,
      });

      setMessage("Pedido de troca enviado com sucesso.");
      setSelectedMyShiftId("");
      setSelectedColleagueShiftId("");
      setSwapReason("");
      setMode("none");
      onRequestCreated();
    } catch (error) {
      setMessage(
        getBackendErrorMessage(error, "Erro ao enviar pedido de troca."),
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  useEffect(() => {
    if (mode === "swap") {
      void loadColleagueShifts();
    }
  }, [mode, weekStart]);

  return (
    <section className="panel-card request-actions-card">
      <div className="panel-header">
        <h2>Ações rápidas</h2>
      </div>

      {message && (
        <div className="dashboard-error compact-error">{message}</div>
      )}

      {mode === "none" && (
        <div className="request-action-grid">
          <button onClick={() => setMode("timeOff")}>
            <span>☀️</span>
            <strong>Pedido de Férias</strong>
            <small>Criar ausência para aprovação</small>
          </button>

          <button onClick={() => setMode("swap")}>
            <span>↔️</span>
            <strong>Pedido de Troca</strong>
            <small>Trocar turno com colega</small>
          </button>
        </div>
      )}

      {mode === "timeOff" && (
        <form className="request-form" onSubmit={submitTimeOff}>
          <div className="request-form-header">
            <strong>Novo Pedido de Férias</strong>
            <button type="button" onClick={() => setMode("none")}>
              Cancelar
            </button>
          </div>

          <label>
            <span>De</span>
            <input
              type="date"
              value={timeOffStart}
              onChange={(event) => {
                setTimeOffStart(event.target.value);

                if (timeOffEnd < event.target.value) {
                  setTimeOffEnd(event.target.value);
                }
              }}
            />
          </label>

          <label>
            <span>Até</span>
            <input
              type="date"
              value={timeOffEnd}
              onChange={(event) => setTimeOffEnd(event.target.value)}
            />
          </label>

          <label className="request-checkbox-row">
            <input
              type="checkbox"
              checked={timeOffAllDay}
              onChange={(event) => setTimeOffAllDay(event.target.checked)}
            />
            <span>Dia inteiro</span>
          </label>

          <label>
            <span>Motivo</span>
            <textarea
              value={timeOffReason}
              onChange={(event) => setTimeOffReason(event.target.value)}
              placeholder="Motivo opcional"
              rows={3}
            />
          </label>

          <button className="primary-button" disabled={isSubmitting}>
            {isSubmitting ? "A enviar..." : "Criar pedido"}
          </button>
        </form>
      )}

      {mode === "swap" && (
        <form className="request-form" onSubmit={submitSwap}>
          <div className="request-form-header">
            <strong>Novo Pedido de Troca</strong>
            <button type="button" onClick={() => setMode("none")}>
              Cancelar
            </button>
          </div>

          <div className="request-info-box">
            Seleciona um dos teus turnos publicados e o turno de um colega que
            queres trocar.
          </div>

          <label>
            <span>Meu Turno</span>
            <select
              value={selectedMyShiftId}
              onChange={(event) =>
                setSelectedMyShiftId(
                  event.target.value === "" ? "" : Number(event.target.value),
                )
              }
            >
              <option value="">Selecionar turno</option>
              {publishedMyShifts.map((shift) => (
                <option value={shift.id} key={shift.id}>
                  {shiftLabel(shift)}
                </option>
              ))}
            </select>
          </label>

          <label>
            <span>Turno do Colega</span>
            <select
              value={selectedColleagueShiftId}
              onChange={(event) =>
                setSelectedColleagueShiftId(
                  event.target.value === "" ? "" : Number(event.target.value),
                )
              }
              disabled={isLoadingColleagueShifts}
            >
              <option value="">
                {isLoadingColleagueShifts
                  ? "A carregar..."
                  : "Selecionar turno"}
              </option>

              {colleagueShifts.map((shift) => (
                <option value={shift.id} key={shift.id}>
                  {shift.user?.name ?? "Colega"} — {shiftLabel(shift)}
                </option>
              ))}
            </select>
          </label>

          <label>
            <span>Motivo</span>
            <textarea
              value={swapReason}
              onChange={(event) => setSwapReason(event.target.value)}
              placeholder="Descreve o motivo da troca (opcional)"
              rows={3}
            />
          </label>

          <button
            className="primary-button"
            disabled={
              isSubmitting ||
              selectedMyShiftId === "" ||
              selectedColleagueShiftId === ""
            }
          >
            {isSubmitting ? "A enviar..." : "Criar pedido"}
          </button>
        </form>
      )}
    </section>
  );
}
