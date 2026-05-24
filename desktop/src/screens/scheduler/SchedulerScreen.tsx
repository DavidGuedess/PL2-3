import { FormEvent, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  createShift,
  deleteShift,
  getMyShifts,
  getShiftTypes,
  getShifts,
  updateShift,
} from "../../api/shiftApi";
import { getUsers } from "../../api/userApi";

import type { Shift, ShiftType } from "../../models/shift";
import type { User } from "../../models/user";

import {
  resolvedEndTime,
  resolvedName,
  resolvedStartTime,
} from "../../models/shift";
import { routes } from "../../navigation/routes";
import { tokenManager } from "../../storage/tokenManager";

import { getBackendErrorMessage } from "../../utils/errorUtils";

type ShiftFormMode = "create" | "edit";

interface ShiftFormState {
  id: number | null;
  userId: number | "";
  date: string;
  startTime: string;
  endTime: string;
  shiftTypeId: number | "";
  published: boolean;
}

const emptyForm: ShiftFormState = {
  id: null,
  userId: "",
  date: "",
  startTime: "09:00",
  endTime: "17:00",
  shiftTypeId: "",
  published: false,
};

function mondayOf(date: Date): Date {
  const copy = new Date(date);
  const day = copy.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  copy.setDate(copy.getDate() + diff);
  return copy;
}

function isoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function addDays(date: Date, days: number): Date {
  const copy = new Date(date);
  copy.setDate(copy.getDate() + days);
  return copy;
}

function formatDay(date: Date): string {
  return new Intl.DateTimeFormat("pt-PT", {
    weekday: "short",
    day: "2-digit",
    month: "short",
  }).format(date);
}

function formatRange(weekStart: Date): string {
  const end = addDays(weekStart, 6);
  return `${isoDate(weekStart)} até ${isoDate(end)}`;
}

function roleLabel(role: string): string {
  if (role === "ADMIN") return "Administrador";
  if (role === "MANAGER") return "Gerente";
  return "Funcionário";
}

export function SchedulerScreen() {
  const navigate = useNavigate();

  const [weekStart, setWeekStart] = useState(mondayOf(new Date()));
  const [shifts, setShifts] = useState<Shift[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [shiftTypes, setShiftTypes] = useState<ShiftType[]>([]);

  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [formMode, setFormMode] = useState<ShiftFormMode>("create");
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [form, setForm] = useState<ShiftFormState>(emptyForm);

  const role = tokenManager.getUserRole();
  const canManage = role === "ADMIN" || role === "MANAGER";

  const weekDays = useMemo(() => {
    return Array.from({ length: 7 }, (_, index) => addDays(weekStart, index));
  }, [weekStart]);

  const shiftsByDay = useMemo(() => {
    const map = new Map<string, Shift[]>();

    for (const day of weekDays) {
      map.set(isoDate(day), []);
    }

    for (const shift of shifts) {
      const key = shift.date.slice(0, 10);
      map.set(key, [...(map.get(key) ?? []), shift]);
    }

    return map;
  }, [shifts, weekDays]);

  async function loadWeek() {
    try {
      setIsLoading(true);
      setError(null);

      const week = isoDate(weekStart);

      const result =
        role === "EMPLOYEE"
          ? await getMyShifts({ week })
          : await getShifts({ week });

      setShifts(
        result.sort((a, b) => {
          return `${a.date}${resolvedStartTime(a)}`.localeCompare(
            `${b.date}${resolvedStartTime(b)}`,
          );
        }),
      );
    } catch (error) {
      setError(getBackendErrorMessage(error, "Erro ao carregar agenda"));
    } finally {
      setIsLoading(false);
    }
  }

  async function loadManagerData() {
    if (!canManage) {
      return;
    }

    try {
      const [usersResult, shiftTypesResult] = await Promise.all([
        getUsers(),
        getShiftTypes(),
      ]);

      setUsers(usersResult.filter((user) => user.active));
      setShiftTypes(shiftTypesResult);
    } catch (error) {
      setError(
        getBackendErrorMessage(
          error,
          "Erro ao carregar dados de gestão de turnos",
        ),
      );
    }
  }

  function openCreateForm(date: string) {
    setFormMode("create");
    setForm({
      ...emptyForm,
      date,
      userId: users[0]?.id ?? "",
      shiftTypeId: shiftTypes[0]?.id ?? "",
    });
    setIsFormOpen(true);
  }

  function openEditForm(shift: Shift) {
    setFormMode("edit");
    setForm({
      id: shift.id,
      userId: shift.userId,
      date: shift.date.slice(0, 10),
      startTime: resolvedStartTime(shift).slice(0, 5),
      endTime: resolvedEndTime(shift).slice(0, 5),
      shiftTypeId: shift.shiftTypeId ?? "",
      published: shift.published,
    });
    setIsFormOpen(true);
  }

  function closeForm() {
    setIsFormOpen(false);
    setForm(emptyForm);
    setError(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!canManage) {
      return;
    }

    if (form.userId === "" || form.date.trim() === "") {
      setError("Seleciona funcionário e data.");
      return;
    }

    try {
      setIsSaving(true);
      setError(null);

      if (formMode === "create") {
        await createShift({
          userId: Number(form.userId),
          date: form.date,
          startTime: form.startTime,
          endTime: form.endTime,
          shiftTypeId:
            form.shiftTypeId === "" ? null : Number(form.shiftTypeId),
        });
      } else if (form.id !== null) {
        await updateShift(form.id, {
          userId: Number(form.userId),
          date: form.date,
          startTime: form.startTime,
          endTime: form.endTime,
          shiftTypeId:
            form.shiftTypeId === "" ? null : Number(form.shiftTypeId),
          published: form.published,
        });
      }

      closeForm();
      await loadWeek();
    } catch (error) {
      setError(
        getBackendErrorMessage(
          error,
          "Erro ao guardar turno. Pode existir conflito para este funcionário/data.",
        ),
      );
    } finally {
      setIsSaving(false);
    }
  }

  async function handleDelete(shift: Shift) {
    const confirmed = window.confirm("Apagar este turno?");

    if (!confirmed) {
      return;
    }

    try {
      setError(null);
      await deleteShift(shift.id);
      await loadWeek();
    } catch (error) {
      setError(getBackendErrorMessage(error, "Erro ao apagar turno."));
    }
  }

  async function togglePublished(shift: Shift) {
    try {
      setError(null);

      const updated = await updateShift(shift.id, {
        published: !shift.published,
      });

      setShifts((current) => {
        return current.map((item) => (item.id === updated.id ? updated : item));
      });
    } catch (error) {
      setError(
        getBackendErrorMessage(error, "Erro ao alterar publicação do turno."),
      );
    }
  }

  useEffect(() => {
    void loadWeek();
  }, [weekStart]);

  useEffect(() => {
    void loadManagerData();
  }, [canManage]);

  return (
    <main className="scheduler-page">
      <section className="scheduler-shell">
        <header className="screen-header dark-header scheduler-header">
          <button
            className="secondary-button"
            onClick={() => navigate(routes.dashboard)}
          >
            Voltar
          </button>

          <div>
            <h1>Agenda</h1>
            <p>{formatRange(weekStart)}</p>
          </div>

          {canManage && (
            <button
              className="primary-small-text-button"
              onClick={() => openCreateForm(isoDate(new Date()))}
            >
              Novo turno
            </button>
          )}
        </header>

        <section className="panel-card scheduler-toolbar">
          <button onClick={() => setWeekStart(addDays(weekStart, -7))}>
            Semana anterior
          </button>

          <button onClick={() => setWeekStart(mondayOf(new Date()))}>
            Hoje
          </button>

          <button onClick={() => setWeekStart(addDays(weekStart, 7))}>
            Próxima semana
          </button>

          <button onClick={loadWeek} disabled={isLoading}>
            {isLoading ? "..." : "Atualizar"}
          </button>
        </section>

        {error && <div className="dashboard-error">{error}</div>}

        {isFormOpen && canManage && (
          <section className="scheduler-form-panel">
            <form className="scheduler-form" onSubmit={handleSubmit}>
              <div className="scheduler-form-header">
                <strong>
                  {formMode === "create" ? "Novo turno" : "Editar turno"}
                </strong>

                <button type="button" onClick={closeForm}>
                  Cancelar
                </button>
              </div>

              <label>
                <span>Funcionário</span>
                <select
                  value={form.userId}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      userId:
                        event.target.value === ""
                          ? ""
                          : Number(event.target.value),
                    }))
                  }
                >
                  <option value="">Selecionar funcionário</option>
                  {users.map((user) => (
                    <option value={user.id} key={user.id}>
                      {user.name} · {user.employeeNumber} ·{" "}
                      {roleLabel(user.role)}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                <span>Data</span>
                <input
                  type="date"
                  value={form.date}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      date: event.target.value,
                    }))
                  }
                />
              </label>

              <label>
                <span>Tipo de turno</span>
                <select
                  value={form.shiftTypeId}
                  onChange={(event) => {
                    const value =
                      event.target.value === ""
                        ? ""
                        : Number(event.target.value);

                    const selectedType = shiftTypes.find(
                      (type) => type.id === value,
                    );

                    setForm((current) => ({
                      ...current,
                      shiftTypeId: value,
                      startTime: selectedType?.startTime ?? current.startTime,
                      endTime: selectedType?.endTime ?? current.endTime,
                    }));
                  }}
                >
                  <option value="">Sem tipo</option>

                  {shiftTypes.map((type) => (
                    <option value={type.id} key={type.id}>
                      {type.name} · {type.startTime}-{type.endTime}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                <span>Início</span>
                <input
                  type="time"
                  value={form.startTime}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      startTime: event.target.value,
                    }))
                  }
                />
              </label>

              <label>
                <span>Fim</span>
                <input
                  type="time"
                  value={form.endTime}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      endTime: event.target.value,
                    }))
                  }
                />
              </label>

              {formMode === "edit" && (
                <label className="scheduler-checkbox">
                  <input
                    type="checkbox"
                    checked={form.published}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        published: event.target.checked,
                      }))
                    }
                  />
                  <span>Publicado</span>
                </label>
              )}

              <button className="primary-button" disabled={isSaving}>
                {isSaving ? "A guardar..." : "Guardar turno"}
              </button>
            </form>
          </section>
        )}

        <section className="scheduler-grid">
          {weekDays.map((day) => {
            const key = isoDate(day);
            const dayShifts = shiftsByDay.get(key) ?? [];

            return (
              <article className="day-column" key={key}>
                <header>
                  <strong>{formatDay(day)}</strong>

                  <div>
                    <span>{dayShifts.length} turno(s)</span>

                    {canManage && (
                      <button onClick={() => openCreateForm(key)}>+</button>
                    )}
                  </div>
                </header>

                {dayShifts.length === 0 ? (
                  <p className="muted-text">Sem turnos</p>
                ) : (
                  <div className="day-shifts">
                    {dayShifts.map((shift) => (
                      <div className="scheduler-shift-card" key={shift.id}>
                        <span>{resolvedName(shift)}</span>

                        <strong>
                          {resolvedStartTime(shift).slice(0, 5)} -{" "}
                          {resolvedEndTime(shift).slice(0, 5)}
                        </strong>

                        <small>{shift.user?.name ?? "Eu"}</small>

                        {!shift.published && <em>Rascunho</em>}

                        {canManage && (
                          <div className="shift-card-actions">
                            <button onClick={() => openEditForm(shift)}>
                              Editar
                            </button>

                            <button onClick={() => togglePublished(shift)}>
                              {shift.published ? "Despublicar" : "Publicar"}
                            </button>

                            <button
                              className="danger-mini"
                              onClick={() => handleDelete(shift)}
                            >
                              Apagar
                            </button>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </article>
            );
          })}
        </section>
      </section>
    </main>
  );
}
