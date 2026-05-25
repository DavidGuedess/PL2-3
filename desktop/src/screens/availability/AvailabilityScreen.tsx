import { FormEvent, useEffect, useMemo, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useNavigate } from "react-router-dom";

import {
  createAvailability,
  deleteAvailability,
  getAvailabilities
} from "../../api/availabilityApi";

import type { Availability } from "../../models/availability";
import { routes } from "../../navigation/routes";
import { tokenManager } from "../../storage/tokenManager";

type AvailabilityType = "PREFERRED" | "UNAVAILABLE";

function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function todayIso(): string {
  return toIsoDate(new Date());
}

function addDaysIso(baseIso: string, days: number): string {
  const date = new Date(baseIso);
  date.setDate(date.getDate() + days);
  return toIsoDate(date);
}

function getMonthStart(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

function getMonthKey(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
}

function formatMonth(date: Date): string {
  return new Intl.DateTimeFormat("pt-PT", {
    month: "long",
    year: "numeric"
  }).format(date);
}

function formatShortDate(dateIso: string): string {
  try {
    return new Intl.DateTimeFormat("pt-PT", {
      weekday: "short",
      day: "2-digit",
      month: "short"
    }).format(new Date(dateIso));
  } catch {
    return dateIso;
  }
}

function typeLabel(type: string): string {
  if (type === "PREFERRED") return "Preferida";
  if (type === "UNAVAILABLE") return "Indisponível";
  return type;
}

function buildCalendarCells(monthDate: Date): Date[] {
  const first = getMonthStart(monthDate);
  const startOffset = first.getDay() === 0 ? 6 : first.getDay() - 1;

  const start = new Date(first);
  start.setDate(first.getDate() - startOffset);

  return Array.from({ length: 42 }, (_, index) => {
    const cell = new Date(start);
    cell.setDate(start.getDate() + index);
    return cell;
  });
}

function getDateRange(startIso: string, endIso: string): string[] {
  const result: string[] = [];
  const current = new Date(startIso);
  const end = new Date(endIso);

  while (current <= end) {
    result.push(toIsoDate(current));
    current.setDate(current.getDate() + 1);
  }

  return result;
}

export function AvailabilityScreen() {
  const navigate = useNavigate();

  const currentUserId = tokenManager.getUserId();
  const currentUserName = tokenManager.getUserName() ?? "Utilizador";

  const [items, setItems] = useState<Availability[]>([]);
  const [selectedDate, setSelectedDate] = useState(todayIso());
  const [displayMonth, setDisplayMonth] = useState(getMonthStart(new Date()));

  const [isAdding, setIsAdding] = useState(false);
  const [type, setType] = useState<AvailabilityType>("UNAVAILABLE");
  const [startDate, setStartDate] = useState(todayIso());
  const [endDate, setEndDate] = useState(todayIso());
  const [note, setNote] = useState("");

  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  // D-4: auto-clear message after 3s
  function showMsg(text: string) {
    setMessage(text);
    setTimeout(() => setMessage(null), 3000);
  }

  const availabilityByDate = useMemo(() => {
    const map = new Map<string, Availability>();

    for (const item of items) {
      map.set(item.date.slice(0, 10), item);
    }

    return map;
  }, [items]);

  const calendarCells = useMemo(() => {
    return buildCalendarCells(displayMonth);
  }, [displayMonth]);

  const listDates = useMemo(() => {
    return Array.from({ length: 28 }, (_, index) => addDaysIso(selectedDate, index));
  }, [selectedDate]);

  async function loadAvailability() {
    try {
      setIsLoading(true);
      setMessage(null);

      const result = await getAvailabilities({
        userId: currentUserId
      });

      setItems(result);
    } catch {
      showMsg("Erro ao carregar disponibilidade.");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const fixedEndDate = endDate < startDate ? startDate : endDate;
    const dates = getDateRange(startDate, fixedEndDate);

    try {
      setIsSaving(true);
      setMessage(null);

      for (const date of dates) {
        await createAvailability({
          date,
          type,
          note: note.trim() || null
        });
      }

      setNote("");
      setIsAdding(false);
      showMsg("Disponibilidade registada com sucesso.");
      await loadAvailability();
    } catch {
      showMsg("Erro ao guardar disponibilidade.");
    } finally {
      setIsSaving(false);
    }
  }

  async function handleDelete(item: Availability) {
    try {
      setMessage(null);

      await deleteAvailability(item.id);
      setItems((current) => current.filter((entry) => entry.id !== item.id));
    } catch {
      showMsg("Erro ao apagar disponibilidade.");
    }
  }

  function previousMonth() {
    setDisplayMonth((current) => {
      return new Date(current.getFullYear(), current.getMonth() - 1, 1);
    });
  }

  function nextMonth() {
    setDisplayMonth((current) => {
      return new Date(current.getFullYear(), current.getMonth() + 1, 1);
    });
  }

  function openAddForDate(dateIso: string) {
    setSelectedDate(dateIso);
    setStartDate(dateIso);
    setEndDate(dateIso);
    setNote("");
    setType("UNAVAILABLE");
    setIsAdding(true);
  }

  useEffect(() => {
    void loadAvailability();
  }, []);

  if (isAdding) {
    return (
      <main className="availability-page">
        <section className="availability-shell narrow">
          <header className="availability-topbar">
            <button className="text-button" onClick={() => setIsAdding(false)}>
              Cancelar
            </button>

            <strong>Adicionar Disponibilidade</strong>

            <button
              className="text-button"
              form="availability-form"
              disabled={isSaving}
            >
              {isSaving ? "..." : "Guardar"}
            </button>
          </header>

          {message && <div className="dashboard-error">{message}</div>}

          <form
            id="availability-form"
            className="availability-editor-card"
            onSubmit={handleSubmit}
          >
            <div className="availability-editor-row">
              <span>Funcionário</span>
              <strong>{currentUserName}</strong>
            </div>

            <label className="availability-radio-row">
              <input
                type="radio"
                checked={type === "UNAVAILABLE"}
                onChange={() => setType("UNAVAILABLE")}
              />
              <span>Indisponível para trabalhar</span>
            </label>

            <label className="availability-radio-row">
              <input
                type="radio"
                checked={type === "PREFERRED"}
                onChange={() => setType("PREFERRED")}
              />
              <span>Preferência para trabalhar</span>
            </label>

            <label className="availability-field-row">
              <span>Data de Início</span>
              <input
                type="date"
                value={startDate}
                onChange={(event) => {
                  setStartDate(event.target.value);

                  if (endDate < event.target.value) {
                    setEndDate(event.target.value);
                  }
                }}
              />
            </label>

            <label className="availability-field-row">
              <span>Data de Fim</span>
              <input
                type="date"
                value={endDate}
                onChange={(event) => setEndDate(event.target.value)}
              />
            </label>

            <label className="availability-note-row">
              <span>Nota</span>
              <textarea
                value={note}
                onChange={(event) => setNote(event.target.value)}
                placeholder="Nota opcional"
                rows={4}
              />
            </label>

            <div className="availability-info-box">
              Para férias, doença ou ausência prolongada, usa também o pedido de férias quando aplicável.
            </div>
          </form>
        </section>
      </main>
    );
  }

  return (
    <main className="availability-page">
      <section className="availability-shell">
        <header className="screen-header dark-header availability-header">
          <button className="secondary-button" onClick={() => navigate(routes.dashboard)}>
            Voltar
          </button>

          <div>
            <h1>A Minha Disponibilidade</h1>
            <p>Preferências e indisponibilidades</p>
          </div>

          <button className="secondary-button" onClick={loadAvailability} disabled={isLoading}>
            {isLoading ? "..." : "Atualizar"}
          </button>
        </header>

        {message && <div className="dashboard-error">{message}</div>}

        <section className="availability-calendar-card">
          <div className="availability-month-header">
            <button onClick={previousMonth}><ChevronLeft size={16} /></button>

            <strong>{formatMonth(displayMonth)}</strong>

            <button onClick={nextMonth}><ChevronRight size={16} /></button>
          </div>

          <div className="availability-weekdays">
            {["Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"].map((day) => (
              <span key={day}>{day}</span>
            ))}
          </div>

          <div className="availability-calendar-grid">
            {calendarCells.map((date) => {
              const dateIso = toIsoDate(date);
              const item = availabilityByDate.get(dateIso);
              const isCurrentMonth = getMonthKey(date) === getMonthKey(displayMonth);
              const isToday = dateIso === todayIso();
              const isSelected = dateIso === selectedDate;

              return (
                <button
                  className={[
                    "availability-day",
                    isCurrentMonth ? "" : "outside",
                    isToday ? "today" : "",
                    isSelected ? "selected" : ""
                  ].join(" ")}
                  key={dateIso}
                  onClick={() => {
                    setSelectedDate(dateIso);

                    if (!isCurrentMonth) {
                      setDisplayMonth(getMonthStart(date));
                    }
                  }}
                  onDoubleClick={() => openAddForDate(dateIso)}
                >
                  <span>{date.getDate()}</span>

                  {item && (
                    <em className={item.type === "PREFERRED" ? "preferred" : "unavailable"} />
                  )}
                </button>
              );
            })}
          </div>
        </section>

        <section className="availability-list-card">
          <div className="panel-header">
            <h2>Próximos dias</h2>

            <button onClick={() => openAddForDate(selectedDate)}>
              Adicionar
            </button>
          </div>

          <div className="availability-days-list">
            {listDates.map((dateIso) => {
              const item = availabilityByDate.get(dateIso);
              const highlighted = dateIso === selectedDate;

              return (
                <article
                  className={`availability-day-row ${highlighted ? "highlighted" : ""}`}
                  key={dateIso}
                >
                  <div>
                    <strong>{formatShortDate(dateIso)}</strong>

                    {item ? (
                      <span className={item.type === "PREFERRED" ? "preferred-text" : "unavailable-text"}>
                        {typeLabel(item.type)}
                      </span>
                    ) : (
                      <span>Sem preferências definidas</span>
                    )}

                    {item?.note && <p>{item.note}</p>}
                  </div>

                  {item ? (
                    <button onClick={() => handleDelete(item)}>
                      Apagar
                    </button>
                  ) : (
                    <button onClick={() => openAddForDate(dateIso)}>
                      +
                    </button>
                  )}
                </article>
              );
            })}
          </div>
        </section>
      </section>
    </main>
  );
}