import { FormEvent, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  createWeekAssignment,
  deleteWeekAssignment,
  getWeekAssignments
} from "../../api/weekAssignmentApi";
import { getUsers } from "../../api/userApi";

import type { User } from "../../models/user";
import type { WeekAssignment } from "../../models/weekAssignment";

import { routes } from "../../navigation/routes";
import { tokenManager } from "../../storage/tokenManager";

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

function formatWeekRange(weekStart: string): string {
  const start = new Date(weekStart);
  const end = addDays(start, 6);

  return `${isoDate(start)} até ${isoDate(end)}`;
}

function roleLabel(role: string): string {
  if (role === "ADMIN") return "Administrador";
  if (role === "MANAGER") return "Gerente";
  return "Funcionário";
}

function categoryLabel(category: string): string {
  if (category === "VETERINARIAN") return "Veterinário";
  if (category === "NURSE") return "Enfermeiro";
  if (category === "ADMINISTRATIVE") return "Administrativo";
  if (category === "OPERATIONAL") return "Operacional";
  return category;
}

export function WeekAssignmentsScreen() {
  const navigate = useNavigate();

  const role = tokenManager.getUserRole();
  const canManage = role === "ADMIN" || role === "MANAGER";

  const [weekStart, setWeekStart] = useState(isoDate(mondayOf(new Date())));
  const [users, setUsers] = useState<User[]>([]);
  const [assignments, setAssignments] = useState<WeekAssignment[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<number | "">("");

  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const availableUsers = useMemo(() => {
    const assignedIds = new Set(assignments.map((assignment) => assignment.userId));

    return users.filter((user) => user.active && !assignedIds.has(user.id));
  }, [users, assignments]);

  async function loadData() {
    if (!canManage) {
      return;
    }

    try {
      setIsLoading(true);
      setMessage(null);

      const [usersResult, assignmentsResult] = await Promise.all([
        getUsers(),
        getWeekAssignments(weekStart)
      ]);

      setUsers(usersResult);
      setAssignments(assignmentsResult);
    } catch {
      setMessage("Erro ao carregar atribuições semanais.");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (selectedUserId === "") {
      setMessage("Seleciona um funcionário.");
      return;
    }

    try {
      setIsSaving(true);
      setMessage(null);

      await createWeekAssignment({
        userId: Number(selectedUserId),
        weekStart
      });

      setSelectedUserId("");
      await loadData();
    } catch {
      setMessage("Erro ao atribuir funcionário à semana.");
    } finally {
      setIsSaving(false);
    }
  }

  async function handleDelete(id: number) {
    try {
      setMessage(null);

      await deleteWeekAssignment(id);
      setAssignments((current) => current.filter((item) => item.id !== id));
    } catch {
      setMessage("Erro ao remover atribuição.");
    }
  }

  function previousWeek() {
    const current = new Date(weekStart);
    setWeekStart(isoDate(addDays(current, -7)));
  }

  function nextWeek() {
    const current = new Date(weekStart);
    setWeekStart(isoDate(addDays(current, 7)));
  }

  function currentWeek() {
    setWeekStart(isoDate(mondayOf(new Date())));
  }

  useEffect(() => {
    void loadData();
  }, [weekStart, canManage]);

  if (!canManage) {
    return (
      <main className="week-page">
        <section className="week-shell">
          <header className="screen-header dark-header">
            <button className="secondary-button" onClick={() => navigate(routes.dashboard)}>
              Voltar
            </button>

            <div>
              <h1>Atribuições Semanais</h1>
              <p>Área reservada à gestão</p>
            </div>
          </header>

          <section className="panel-card">
            <p className="muted-text">
              Não tens permissões para consultar esta área.
            </p>
          </section>
        </section>
      </main>
    );
  }

  return (
    <main className="week-page">
      <section className="week-shell">
        <header className="screen-header dark-header week-header">
          <button className="secondary-button" onClick={() => navigate(routes.dashboard)}>
            Voltar
          </button>

          <div>
            <h1>Atribuições Semanais</h1>
            <p>{formatWeekRange(weekStart)}</p>
          </div>

          <button className="secondary-button" onClick={loadData} disabled={isLoading}>
            {isLoading ? "..." : "Atualizar"}
          </button>
        </header>

        <section className="panel-card week-toolbar">
          <button onClick={previousWeek}>Semana anterior</button>
          <button onClick={currentWeek}>Semana atual</button>
          <button onClick={nextWeek}>Próxima semana</button>
        </section>

        {message && <div className="dashboard-error">{message}</div>}

        <section className="week-layout">
          <form className="panel-card week-form" onSubmit={handleCreate}>
            <h2>Atribuir funcionário</h2>

            <label>
              <span>Semana</span>
              <input
                type="date"
                value={weekStart}
                onChange={(event) => setWeekStart(event.target.value)}
              />
            </label>

            <label>
              <span>Funcionário</span>
              <select
                value={selectedUserId}
                onChange={(event) =>
                  setSelectedUserId(
                    event.target.value === "" ? "" : Number(event.target.value)
                  )
                }
              >
                <option value="">Selecionar funcionário</option>

                {availableUsers.map((user) => (
                  <option value={user.id} key={user.id}>
                    {user.name} · {user.employeeNumber}
                  </option>
                ))}
              </select>
            </label>

            <button className="primary-button" disabled={isSaving || selectedUserId === ""}>
              {isSaving ? "A guardar..." : "Atribuir"}
            </button>
          </form>

          <section className="panel-card week-list-card">
            <div className="panel-header">
              <h2>Funcionários atribuídos</h2>
              <span className="week-count">{assignments.length}</span>
            </div>

            {isLoading ? (
              <p className="muted-text">A carregar...</p>
            ) : assignments.length === 0 ? (
              <p className="muted-text">Sem funcionários atribuídos a esta semana.</p>
            ) : (
              <div className="week-assignment-list">
                {assignments.map((assignment) => (
                  <article className="week-assignment-row" key={assignment.id}>
                    <div className="week-user-avatar">
                      {assignment.user.name.charAt(0).toUpperCase()}
                    </div>

                    <div>
                      <strong>{assignment.user.name}</strong>
                      <span>
                        {assignment.user.employeeNumber} · {roleLabel(assignment.user.role)}
                      </span>
                      <small>{categoryLabel(assignment.user.category)}</small>
                    </div>

                    <button onClick={() => handleDelete(assignment.id)}>
                      Remover
                    </button>
                  </article>
                ))}
              </div>
            )}
          </section>
        </section>
      </section>
    </main>
  );
}