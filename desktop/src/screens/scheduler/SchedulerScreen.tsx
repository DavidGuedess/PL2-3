import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  ChevronLeft,
  ChevronRight,
  Check,
  Copy,
  ClipboardPaste,
} from "lucide-react";
import {
  createShift,
  deleteShift,
  getMyShifts,
  getShiftTypes,
  getShifts,
  updateShift,
} from "../../api/shiftApi";
import { getUsers } from "../../api/userApi";
import { getTimeOffRequests } from "../../api/requestApi";
import {
  resolvedEndTime,
  resolvedName,
  resolvedStartTime,
} from "../../models/shift";
import type { Shift, ShiftType } from "../../models/shift";
import type { User } from "../../models/user";
import type { TimeOffRequest } from "../../models/request";
import { tokenManager } from "../../storage/tokenManager";
import { getBackendErrorMessage } from "../../utils/errorUtils";

function monday(date: Date): Date {
  const d = new Date(date);
  const day = d.getDay();
  d.setDate(d.getDate() + (day === 0 ? -6 : 1 - day));
  return d;
}
function iso(d: Date) {
  return d.toISOString().slice(0, 10);
}
function addDays(d: Date, n: number) {
  const c = new Date(d);
  c.setDate(c.getDate() + n);
  return c;
}
function fmtDay(d: Date) {
  return new Intl.DateTimeFormat("pt-PT", { weekday: "short" }).format(d);
}
function roleLabel(r: string) {
  if (r === "ADMIN") return "Admin";
  if (r === "MANAGER") return "Gerente";
  return "Func.";
}

function initials(name: string) {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join("");
}

type ModalState =
  | { kind: "none" }
  | { kind: "view"; shift: Shift }
  | { kind: "create"; userId: number; date: string }
  | { kind: "edit"; shift: Shift };

interface FormState {
  userId: number | "";
  date: string;
  startTime: string;
  endTime: string;
  shiftTypeId: number | "";
  published: boolean;
}

const emptyForm: FormState = {
  userId: "",
  date: "",
  startTime: "09:00",
  endTime: "17:00",
  shiftTypeId: "",
  published: false,
};

export function SchedulerScreen() {
  const role = tokenManager.getUserRole();
  const canManage = role === "ADMIN" || role === "MANAGER";

  const [weekStart, setWeekStart] = useState(monday(new Date()));
  const [shifts, setShifts] = useState<Shift[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [types, setTypes] = useState<ShiftType[]>([]);
  const [timeOffs, setTimeOffs] = useState<TimeOffRequest[]>([]);
  const [search, setSearch] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [modal, setModal] = useState<ModalState>({ kind: "none" });
  const [form, setForm] = useState<FormState>(emptyForm);
  const [toast, setToast] = useState<string | null>(null);
  const [copiedWeek, setCopiedWeek] = useState<{
    fromWeek: string;
    shifts: Shift[];
  } | null>(null);
  const [isPasting, setIsPasting] = useState(false);

  const weekDays = useMemo(
    () => Array.from({ length: 7 }, (_, i) => addDays(weekStart, i)),
    [weekStart],
  );

  const today = new Date().toISOString().slice(0, 10);

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  }

  async function loadWeek() {
    setIsLoading(true);
    setError(null);
    try {
      const week = iso(weekStart);
      const result =
        role === "EMPLOYEE"
          ? await getMyShifts({ week })
          : await getShifts({ week });
      setShifts(
        result.sort((a, b) =>
          `${a.date}${resolvedStartTime(a)}`.localeCompare(
            `${b.date}${resolvedStartTime(b)}`,
          ),
        ),
      );
    } catch (e) {
      setError(getBackendErrorMessage(e, "Erro ao carregar agenda"));
    } finally {
      setIsLoading(false);
    }
  }

  async function loadMeta() {
    if (!canManage) return;
    const [u, t, tor] = await Promise.allSettled([
      getUsers(),
      getShiftTypes(),
      getTimeOffRequests(),
    ]);
    if (u.status === "fulfilled") setUsers(u.value.filter((x) => x.active));
    if (t.status === "fulfilled") setTypes(t.value);
    if (tor.status === "fulfilled") setTimeOffs(tor.value);
  }

  useEffect(() => {
    void loadWeek();
  }, [weekStart]);
  useEffect(() => {
    void loadMeta();
  }, []);

  // Build rows: admin/manager sees all users; employee sees only self
  const rows = useMemo(() => {
    if (!canManage) {
      const myId = tokenManager.getUserId();
      const me =
        users.find((u) => u.id === myId) ??
        ({
          id: myId,
          name: tokenManager.getUserName() ?? "Eu",
          employeeNumber: tokenManager.getEmployeeNumber() ?? "",
          role: role ?? "EMPLOYEE",
          category: tokenManager.getUserCategory() ?? "",
          email: "",
          active: true,
        } as User);
      return [me];
    }
    const q = search.toLowerCase();
    return users.filter(
      (u) =>
        !q || u.name.toLowerCase().includes(q) || u.employeeNumber.includes(q),
    );
  }, [users, search, canManage]);

  const shiftsByUserDay = useMemo(() => {
    const map = new Map<string, Shift[]>();
    for (const s of shifts) {
      const key = `${s.userId}-${s.date.slice(0, 10)}`;
      map.set(key, [...(map.get(key) ?? []), s]);
    }
    return map;
  }, [shifts]);

  const approvedTimeOffs = useMemo(
    () => timeOffs.filter((r) => r.status === "APPROVED"),
    [timeOffs],
  );

  function hasTimeOff(userId: number, date: string) {
    const d = new Date(date);
    return approvedTimeOffs.some(
      (r) =>
        r.userId === userId &&
        new Date(r.startDate) <= d &&
        new Date(r.endDate) >= d,
    );
  }

  // ---- Form helpers ----
  function openCreate(userId: number, date: string) {
    setForm({ ...emptyForm, userId, date, shiftTypeId: types[0]?.id ?? "" });
    setModal({ kind: "create", userId, date });
  }

  function openEdit(shift: Shift) {
    setForm({
      userId: shift.userId,
      date: shift.date.slice(0, 10),
      startTime: resolvedStartTime(shift).slice(0, 5),
      endTime: resolvedEndTime(shift).slice(0, 5),
      shiftTypeId: shift.shiftTypeId ?? "",
      published: shift.published,
    });
    setModal({ kind: "edit", shift });
  }

  function closeModal() {
    setModal({ kind: "none" });
    setError(null);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!canManage || form.userId === "") return;
    setIsSaving(true);
    setError(null);
    try {
      if (modal.kind === "create") {
        await createShift({
          userId: Number(form.userId),
          date: form.date,
          startTime: form.startTime,
          endTime: form.endTime,
          shiftTypeId:
            form.shiftTypeId === "" ? null : Number(form.shiftTypeId),
        });
        showToast("Turno criado com sucesso.");
      } else if (modal.kind === "edit") {
        await updateShift((modal as { kind: "edit"; shift: Shift }).shift.id, {
          userId: Number(form.userId),
          date: form.date,
          startTime: form.startTime,
          endTime: form.endTime,
          shiftTypeId:
            form.shiftTypeId === "" ? null : Number(form.shiftTypeId),
          published: form.published,
        });
        showToast("Turno atualizado.");
      }
      closeModal();
      await loadWeek();
    } catch (e) {
      setError(
        getBackendErrorMessage(e, "Conflito de horário ou erro ao guardar."),
      );
    } finally {
      setIsSaving(false);
    }
  }

  async function handleDelete(shift: Shift) {
    if (!window.confirm("Apagar este turno?")) return;
    try {
      await deleteShift(shift.id);
      showToast("Turno apagado.");
      await loadWeek();
    } catch (e) {
      setError(getBackendErrorMessage(e, "Erro ao apagar turno."));
    }
  }

  async function handleTogglePublish(shift: Shift) {
    try {
      const updated = await updateShift(shift.id, {
        published: !shift.published,
      });
      setShifts((prev) => prev.map((s) => (s.id === updated.id ? updated : s)));
      showToast(updated.published ? "Turno publicado." : "Turno despublicado.");
    } catch (e) {
      setError(getBackendErrorMessage(e, "Erro ao alterar publicação."));
    }
  }

  async function publishAll() {
    const drafts = shifts.filter((s) => !s.published);
    if (drafts.length === 0) return;
    if (!window.confirm(`Publicar ${drafts.length} turnos em rascunho?`))
      return;
    try {
      await Promise.all(
        drafts.map((s) => updateShift(s.id, { published: true })),
      );
      showToast(`${drafts.length} turnos publicados.`);
      await loadWeek();
    } catch (e) {
      setError(getBackendErrorMessage(e, "Erro ao publicar turnos."));
    }
  }

  function copyWeek() {
    if (shifts.length === 0) return;
    setCopiedWeek({ fromWeek: iso(weekStart), shifts: [...shifts] });
    showToast(`${shifts.length} turno(s) copiados.`);
  }

  async function pasteWeek() {
    if (!copiedWeek || isPasting) return;
    const sourceMonday = monday(new Date(copiedWeek.fromWeek + "T12:00"));
    const targetMonday = weekStart;

    // Map each copied shift to the same weekday in the target week
    const toCreate = copiedWeek.shifts.map((s) => {
      const shiftDate = new Date(s.date.slice(0, 10) + "T12:00");
      const sourceDay = monday(shiftDate);
      const dayOffset = Math.round(
        (shiftDate.getTime() - sourceMonday.getTime()) / 86_400_000,
      );
      const targetDate = iso(addDays(targetMonday, dayOffset));
      return {
        userId: s.userId,
        date: targetDate,
        startTime: resolvedStartTime(s),
        endTime: resolvedEndTime(s),
        shiftTypeId: s.shiftTypeId ?? null,
      };
    });

    setIsPasting(true);
    setError(null);
    let created = 0,
      failed = 0;
    for (const body of toCreate) {
      try {
        await createShift(body);
        created++;
      } catch {
        failed++;
      }
    }
    setIsPasting(false);
    await loadWeek();
    if (failed === 0) showToast(`${created} turno(s) colados como rascunho.`);
    else showToast(`${created} colados, ${failed} falharam (conflito).`);
  }

  const draftCount = shifts.filter((s) => !s.published).length;
  const currentWeekKey = iso(weekStart);
  const canPaste =
    copiedWeek !== null && copiedWeek.fromWeek !== currentWeekKey;

  const viewShift = modal.kind === "view" ? modal.shift : null;
  const isFormModal = modal.kind === "create" || modal.kind === "edit";

  return (
    <div className="scheduler-page">
      {/* Toolbar */}
      <div className="scheduler-toolbar">
        <div className="week-nav">
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => setWeekStart(addDays(weekStart, -7))}
          >
            <ChevronLeft size={15} />
          </button>
          <span className="week-nav-label">
            {iso(weekStart)} — {iso(addDays(weekStart, 6))}
          </span>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => setWeekStart(addDays(weekStart, 7))}
          >
            <ChevronRight size={15} />
          </button>
          <button
            className="btn btn-ghost btn-sm"
            onClick={() => setWeekStart(monday(new Date()))}
          >
            Hoje
          </button>
        </div>

        <button
          className="btn btn-secondary btn-sm"
          onClick={loadWeek}
          disabled={isLoading}
        >
          {isLoading ? "..." : "↻ Atualizar"}
        </button>

        {canManage && (
          <div className="scheduler-toolbar-right">
            <button
              className="btn btn-ghost btn-sm"
              onClick={copyWeek}
              disabled={shifts.length === 0}
              title="Copiar todos os turnos desta semana"
            >
              <Copy size={14} /> Copiar semana
            </button>

            {canPaste && (
              <button
                className="btn btn-ghost btn-sm"
                onClick={() => void pasteWeek()}
                disabled={isPasting}
                title={`Colar ${copiedWeek!.shifts.length} turno(s) da semana de ${copiedWeek!.fromWeek}`}
                style={{ color: "var(--accent)", borderColor: "var(--accent)" }}
              >
                <ClipboardPaste size={14} />
                {isPasting
                  ? "A colar..."
                  : `Colar (${copiedWeek!.shifts.length})`}
              </button>
            )}

            {draftCount > 0 && (
              <button className="btn btn-secondary btn-sm" onClick={publishAll}>
                Publicar {draftCount} rascunho(s)
              </button>
            )}
            <button
              className="btn btn-primary btn-sm"
              onClick={() => openCreate(users[0]?.id ?? 0, iso(new Date()))}
            >
              + Novo turno
            </button>
          </div>
        )}
      </div>

      {error && (
        <div className="error-banner" style={{ margin: "8px 16px 0" }}>
          {error}
        </div>
      )}

      {/* Grid */}
      <div className="scheduler-grid-wrapper">
        <div className="scheduler-grid">
          {/* Header row */}
          <div className="scheduler-header-row">
            <div className="col-employee">
              {canManage && (
                <input
                  placeholder="Pesquisar..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
              )}
            </div>
            {weekDays.map((day) => {
              const key = iso(day);
              const isToday = key === today;
              return (
                <div
                  key={key}
                  className={`col-day${isToday ? " is-today" : ""}`}
                >
                  <div className="day-head-label">{fmtDay(day)}</div>
                  <div
                    className={`day-head-num${isToday ? " today-circle" : ""}`}
                  >
                    {day.getDate()}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Rows */}
          {rows.map((user) => (
            <div key={user.id} className="sched-row">
              <div className="employee-cell">
                <div className="user-avatar sm">{initials(user.name)}</div>
                <div className="employee-cell-text">
                  <strong>{user.name}</strong>
                  <span>
                    {user.employeeNumber} · {roleLabel(user.role)}
                  </span>
                </div>
              </div>

              {weekDays.map((day) => {
                const key = iso(day);
                const isToday = key === today;
                const dayShifts =
                  shiftsByUserDay.get(`${user.id}-${key}`) ?? [];
                const onLeave = hasTimeOff(user.id, key);

                return (
                  <div
                    key={key}
                    className={`day-cell${isToday ? " is-today" : ""}`}
                  >
                    {canManage && (
                      <button
                        className="day-cell-add"
                        onClick={() => openCreate(user.id, key)}
                      >
                        +
                      </button>
                    )}

                    {onLeave && (
                      <div className="shift-chip holiday">
                        <div className="shift-chip-time">🌴 Folga</div>
                      </div>
                    )}

                    {dayShifts.map((shift) => (
                      <div
                        key={shift.id}
                        className={`shift-chip${shift.published ? "" : " draft"}`}
                        onClick={() => setModal({ kind: "view", shift })}
                      >
                        <div className="shift-chip-time">
                          {resolvedStartTime(shift).slice(0, 5)} —{" "}
                          {resolvedEndTime(shift).slice(0, 5)}
                        </div>
                        <div className="shift-chip-name">
                          {resolvedName(shift)}
                        </div>
                        {!shift.published && (
                          <div className="shift-chip-draft">Rascunho</div>
                        )}
                      </div>
                    ))}
                  </div>
                );
              })}
            </div>
          ))}

          {rows.length === 0 && !isLoading && (
            <div className="history-empty">Nenhum funcionário encontrado.</div>
          )}
        </div>
      </div>

      {/* View shift modal */}
      {viewShift && (
        <div className="shift-modal-overlay" onClick={closeModal}>
          <div className="shift-modal" onClick={(e) => e.stopPropagation()}>
            <h3>Detalhes do turno</h3>
            <div className="shift-detail-info">
              <div className="shift-detail-row">
                <span>Funcionário</span>
                <strong>
                  {viewShift.user?.name ?? `ID ${viewShift.userId}`}
                </strong>
              </div>
              <div className="shift-detail-row">
                <span>Data</span>
                <strong>{viewShift.date.slice(0, 10)}</strong>
              </div>
              <div className="shift-detail-row">
                <span>Horário</span>
                <strong>
                  {resolvedStartTime(viewShift).slice(0, 5)} —{" "}
                  {resolvedEndTime(viewShift).slice(0, 5)}
                </strong>
              </div>
              <div className="shift-detail-row">
                <span>Tipo</span>
                <strong>{resolvedName(viewShift)}</strong>
              </div>
              <div className="shift-detail-row">
                <span>Estado</span>
                <strong>
                  {viewShift.published ? "Publicado" : "Rascunho"}
                </strong>
              </div>
            </div>

            {canManage && (
              <div className="shift-modal-actions">
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => openEdit(viewShift)}
                >
                  Editar
                </button>
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => {
                    void handleTogglePublish(viewShift);
                    closeModal();
                  }}
                >
                  {viewShift.published ? "Despublicar" : "Publicar"}
                </button>
                <button
                  className="btn btn-danger btn-sm"
                  onClick={() => {
                    void handleDelete(viewShift);
                    closeModal();
                  }}
                >
                  Apagar
                </button>
              </div>
            )}

            <div
              className="form-actions"
              style={{ paddingTop: 12, marginTop: 0, borderTop: "none" }}
            >
              <button className="btn btn-ghost btn-sm" onClick={closeModal}>
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Create / Edit modal */}
      {isFormModal && (
        <div className="shift-modal-overlay" onClick={closeModal}>
          <div className="shift-modal" onClick={(e) => e.stopPropagation()}>
            <h3>{modal.kind === "create" ? "Novo turno" : "Editar turno"}</h3>

            {error && <div className="error-banner">{error}</div>}

            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Funcionário</label>
                <select
                  value={form.userId}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, userId: Number(e.target.value) }))
                  }
                  required
                >
                  <option value="">Selecionar...</option>
                  {users.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.name} · {u.employeeNumber}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Data</label>
                <input
                  type="date"
                  value={form.date}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, date: e.target.value }))
                  }
                  required
                />
              </div>

              <div className="form-group">
                <label>Tipo de turno</label>
                <select
                  value={form.shiftTypeId}
                  onChange={(e) => {
                    const val =
                      e.target.value === "" ? "" : Number(e.target.value);
                    const t = types.find((t) => t.id === val);
                    setForm((f) => ({
                      ...f,
                      shiftTypeId: val,
                      startTime: t?.startTime ?? f.startTime,
                      endTime: t?.endTime ?? f.endTime,
                    }));
                  }}
                >
                  <option value="">Sem tipo</option>
                  {types.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name} · {t.startTime}–{t.endTime}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Início</label>
                  <input
                    type="time"
                    value={form.startTime}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, startTime: e.target.value }))
                    }
                  />
                </div>
                <div className="form-group">
                  <label>Fim</label>
                  <input
                    type="time"
                    value={form.endTime}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, endTime: e.target.value }))
                    }
                  />
                </div>
              </div>

              {modal.kind === "edit" && (
                <div
                  className="form-group"
                  style={{ display: "flex", alignItems: "center", gap: 8 }}
                >
                  <input
                    type="checkbox"
                    id="pub"
                    checked={form.published}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, published: e.target.checked }))
                    }
                    style={{ width: "auto" }}
                  />
                  <label
                    htmlFor="pub"
                    style={{
                      textTransform: "none",
                      letterSpacing: 0,
                      fontSize: 14,
                    }}
                  >
                    Publicado
                  </label>
                </div>
              )}

              <div className="form-actions">
                <button
                  type="button"
                  className="btn btn-ghost btn-sm"
                  onClick={closeModal}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="btn btn-primary btn-sm"
                  disabled={isSaving}
                >
                  {isSaving ? "A guardar..." : "Guardar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Toast */}
      {toast && (
        <div className="toast-container">
          <div className="toast">
            <Check size={15} /> {toast}
          </div>
        </div>
      )}
    </div>
  );
}
