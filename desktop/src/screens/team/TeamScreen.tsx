import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import { activateUser, deactivateUser, getUsers } from "../../api/userApi";
import type { User } from "../../models/user";
import { routes } from "../../navigation/routes";
import { tokenManager } from "../../storage/tokenManager";

import { getBackendErrorMessage } from "../../utils/errorUtils";

function initials(name: string): string {
  return (
    name
      .split(" ")
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p[0].toUpperCase())
      .join("") || "?"
  );
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

export function TeamScreen() {
  const navigate = useNavigate();

  const [users, setUsers] = useState<User[]>([]);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [query, setQuery] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isActionLoading, setIsActionLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const currentRole = tokenManager.getUserRole();
  const canViewTeam = currentRole === "ADMIN" || currentRole === "MANAGER";
  const canManage = currentRole === "ADMIN";

  const filteredUsers = useMemo(() => {
    const q = query.trim().toLowerCase();

    if (!q) {
      return users;
    }

    return users.filter((user) => {
      return (
        user.name.toLowerCase().includes(q) ||
        user.employeeNumber.toLowerCase().includes(q) ||
        user.email.toLowerCase().includes(q)
      );
    });
  }, [users, query]);

  async function loadUsers() {
    if (!canViewTeam) {
      return;
    }

    try {
      setIsLoading(true);
      setError(null);

      const result = await getUsers();
      setUsers(result);
    } catch (error) {
      setError(getBackendErrorMessage(error, "Erro ao carregar equipa"));
    } finally {
      setIsLoading(false);
    }
  }

  async function toggleActive(user: User) {
    try {
      setIsActionLoading(true);
      setError(null);

      const updated = user.active
        ? await deactivateUser(user.id)
        : await activateUser(user.id);

      setUsers((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      );
      setSelectedUser(updated);
    } catch (error) {
      setError(
        getBackendErrorMessage(error, "Erro ao alterar estado do funcionário"),
      );
    } finally {
      setIsActionLoading(false);
    }
  }

  useEffect(() => {
    void loadUsers();
  }, []);

  if (selectedUser) {
    return (
      <main className="team-page">
        <section className="team-shell">
          <header className="screen-header dark-header">
            <button
              className="secondary-button"
              onClick={() => setSelectedUser(null)}
            >
              Voltar
            </button>

            <div>
              <h1>{selectedUser.name}</h1>
              <p>{selectedUser.employeeNumber}</p>
            </div>
          </header>

          {error && <div className="dashboard-error">{error}</div>}

          <section className="employee-detail-card">
            <div className="team-avatar large">
              {initials(selectedUser.name)}
            </div>

            <h2>{selectedUser.name}</h2>
            <p>
              {roleLabel(selectedUser.role)} ·{" "}
              {categoryLabel(selectedUser.category)}
            </p>

            <div className="detail-list">
              <div>
                <span>Email</span>
                <strong>{selectedUser.email}</strong>
              </div>
              <div>
                <span>Contacto</span>
                <strong>{selectedUser.contact ?? "Não definido"}</strong>
              </div>
              <div>
                <span>Estado</span>
                <strong>{selectedUser.active ? "Ativo" : "Inativo"}</strong>
              </div>
            </div>

            <div className="detail-actions">
              <button onClick={() => navigate(routes.scheduler)}>
                Ver agenda
              </button>

              <button
                onClick={() =>
                  navigate(`${routes.inbox}?dm=${selectedUser.id}`)
                }
              >
                Mensagem
              </button>

              {canManage && (
                <button
                  className={
                    selectedUser.active ? "danger-button" : "success-button"
                  }
                  onClick={() => toggleActive(selectedUser)}
                  disabled={isActionLoading}
                >
                  {isActionLoading
                    ? "A processar..."
                    : selectedUser.active
                      ? "Desativar"
                      : "Reativar"}
                </button>
              )}
            </div>
          </section>
        </section>
      </main>
    );
  }
  if (!canViewTeam) {
    return (
      <main className="team-page">
        <section className="team-shell">
          <header className="screen-header dark-header">
            <button
              className="secondary-button"
              onClick={() => navigate(routes.dashboard)}
            >
              Voltar
            </button>

            <div>
              <h1>A Minha Equipa</h1>
              <p>Área reservada à gestão</p>
            </div>
          </header>

          <section className="panel-card">
            <p className="muted-text">
              Não tens permissões para consultar a equipa.
            </p>
          </section>
        </section>
      </main>
    );
  }

  return (
    <main className="team-page">
      <section className="team-shell">
        <header className="screen-header dark-header">
          <button
            className="secondary-button"
            onClick={() => navigate(routes.dashboard)}
          >
            Voltar
          </button>

          <div>
            <h1>A Minha Equipa</h1>
            <p>Funcionários registados no sistema</p>
          </div>
        </header>

        <section className="panel-card team-toolbar">
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Pesquisar por nome, email ou número"
          />

          <button onClick={loadUsers} disabled={isLoading}>
            {isLoading ? "A carregar..." : "Atualizar"}
          </button>
        </section>

        {error && <div className="dashboard-error">{error}</div>}

        {isLoading ? (
          <section className="panel-card">
            <p className="muted-text">A carregar equipa...</p>
          </section>
        ) : (
          <section className="team-grid">
            {filteredUsers.map((user) => (
              <button
                className="team-card"
                key={user.id}
                onClick={() => setSelectedUser(user)}
              >
                <div className="team-avatar">{initials(user.name)}</div>

                <strong>{user.name}</strong>
                <span>{user.employeeNumber}</span>
                <small>
                  {roleLabel(user.role)} · {categoryLabel(user.category)}
                </small>

                {!user.active && <em>Inativo</em>}
              </button>
            ))}
          </section>
        )}
      </section>
    </main>
  );
}
