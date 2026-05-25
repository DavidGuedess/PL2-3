import { FormEvent, useEffect, useMemo, useState } from "react";
import { X, Check } from "lucide-react";
import { useNavigate } from "react-router-dom";

import { activateUser, deactivateUser, getUsers, updateUser } from "../../api/userApi";
import { UserAvatar } from "../../components/UserAvatar";
import { getTimeOffRequests } from "../../api/requestApi";
import type { User } from "../../models/user";
import type { TimeOffRequest } from "../../models/request";
import { routes } from "../../navigation/routes";
import { tokenManager } from "../../storage/tokenManager";
import { getBackendErrorMessage } from "../../utils/errorUtils";

function initials(name: string) {
  return name.split(" ").filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join("") || "?";
}

function roleLabel(r: string) {
  if (r === "ADMIN")   return "Administrador";
  if (r === "MANAGER") return "Gerente";
  return "Funcionário";
}

function catLabel(c: string) {
  if (c === "VETERINARIAN")  return "Veterinário";
  if (c === "NURSE")         return "Enfermeiro";
  if (c === "ADMINISTRATIVE") return "Administrativo";
  if (c === "OPERATIONAL")   return "Operacional";
  return c;
}

function fmtDate(d: string) {
  return new Intl.DateTimeFormat("pt-PT", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(d.slice(0, 10) + "T12:00"));
}

export function TeamScreen() {
  const navigate = useNavigate();
  const role      = tokenManager.getUserRole() ?? "EMPLOYEE";
  const isAdmin   = role === "ADMIN";
  const isManager = role === "MANAGER";
  const canView   = isAdmin || isManager;
  const currentId = tokenManager.getUserId();

  const [users,        setUsers]        = useState<User[]>([]);
  const [timeOffs,     setTimeOffs]     = useState<TimeOffRequest[]>([]);
  const [selected,     setSelected]     = useState<User | null>(null);
  const [tab,          setTab]          = useState<"active" | "archived">("active");
  const [query,        setQuery]        = useState("");
  const [isLoading,    setIsLoading]    = useState(false);
  const [isActLoading, setIsActLoading] = useState(false);
  const [error,        setError]        = useState<string | null>(null);
  const [toast,        setToast]        = useState<string | null>(null);

  // Edit panel state
  const [showEdit,     setShowEdit]     = useState(false);
  const [editForm,     setEditForm]     = useState({ name: "", email: "", contact: "", role: "", category: "" });
  const [isSaving,     setIsSaving]     = useState(false);

  // Vacations panel state
  const [showVacations, setShowVacations] = useState(false);

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  }

  async function loadAll() {
    if (!canView) return;
    setIsLoading(true); setError(null);
    try {
      const [u, tor] = await Promise.allSettled([getUsers(), getTimeOffRequests()]);
      if (u.status   === "fulfilled") setUsers(u.value);
      if (tor.status === "fulfilled") setTimeOffs(tor.value);
    } catch (e) {
      setError(getBackendErrorMessage(e, "Erro ao carregar equipa"));
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => { void loadAll(); }, []);

  const displayed = useMemo(() => {
    const q = query.toLowerCase().trim();
    return users
      .filter(u => (tab === "active" ? u.active : !u.active))
      .filter(u => !q || u.name.toLowerCase().includes(q) || u.email.toLowerCase().includes(q) || u.employeeNumber.includes(q));
  }, [users, tab, query]);

  async function toggleActive(user: User) {
    setIsActLoading(true); setError(null);
    try {
      const updated = user.active ? await deactivateUser(user.id) : await activateUser(user.id);
      setUsers(prev => prev.map(u => u.id === updated.id ? updated : u));
      setSelected(updated);
      showToast(updated.active ? "Conta reativada." : "Conta desativada.");
    } catch (e) {
      setError(getBackendErrorMessage(e, "Erro ao alterar estado."));
    } finally {
      setIsActLoading(false);
    }
  }

  function openEdit(user: User) {
    setEditForm({ name: user.name, email: user.email, contact: user.contact ?? "", role: user.role, category: user.category });
    setShowEdit(true);
  }

  async function handleEditSubmit(e: FormEvent) {
    e.preventDefault();
    if (!selected) return;
    setIsSaving(true);
    try {
      const updated = await updateUser(selected.id, {
        name:     editForm.name     || undefined,
        email:    editForm.email    || undefined,
        contact:  editForm.contact  || undefined,
        role:     editForm.role     || undefined,
        category: editForm.category || undefined,
      });
      setUsers(prev => prev.map(u => u.id === updated.id ? updated : u));
      setSelected(updated);
      setShowEdit(false);
      showToast("Perfil atualizado.");
    } catch (e) {
      setError(getBackendErrorMessage(e, "Erro ao atualizar."));
    } finally {
      setIsSaving(false);
    }
  }

  const userVacations = selected
    ? timeOffs.filter(r => r.userId === selected.id && r.status === "APPROVED")
    : [];

  async function removeVacation(id: number) {
    const { updateTimeOffRequestStatus } = await import("../../api/requestApi");
    try {
      await updateTimeOffRequestStatus(id, { status: "REJECTED" });
      setTimeOffs(prev => prev.map(r => r.id === id ? { ...r, status: "REJECTED" } : r));
      showToast("Férias removidas.");
    } catch {
      showToast("Erro ao remover férias.");
    }
  }

  if (!canView) {
    return (
      <div className="team-page">
        <div className="page-header"><h1>Equipa</h1></div>
        <div className="panel-card">
          <p className="muted-text">Não tens permissão para ver a equipa.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="team-page">
      <div className="page-header">
        <h1>Pessoas</h1>
        {isAdmin && (
          <button className="btn btn-primary" onClick={() => navigate(routes.profile)}>
            + Novo Membro
          </button>
        )}
      </div>

      <div className="page-tabs">
        <button className={`page-tab${tab === "active" ? " active" : ""}`} onClick={() => setTab("active")}>
          Ativo
        </button>
        <button className={`page-tab${tab === "archived" ? " active" : ""}`} onClick={() => setTab("archived")}>
          Arquivado
        </button>
      </div>

      <div className="page-toolbar">
        <input
          className="search-input"
          placeholder="Pesquisar por nome, email ou número..."
          value={query}
          onChange={e => setQuery(e.target.value)}
        />
        <button className="btn btn-secondary btn-sm" onClick={loadAll} disabled={isLoading}>
          {isLoading ? "..." : "↻ Atualizar"}
        </button>
        <span style={{ marginLeft: "auto", fontSize: 13, color: "var(--text-muted)" }}>
          {displayed.length} resultado(s)
        </span>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {isLoading ? (
        <div className="loading-state">A carregar...</div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Nome</th>
              <th>Posição</th>
              <th>Role</th>
              <th>Estado</th>
              <th style={{ textAlign: "right" }}>Ações</th>
            </tr>
          </thead>
          <tbody>
            {displayed.map(user => (
              <tr key={user.id} onClick={() => { setSelected(user); setShowEdit(false); setShowVacations(false); }}>
                <td>
                  <div className="user-cell">
                    <UserAvatar name={user.name} profilePicture={user.profilePicture} />
                    <div className="user-info">
                      <strong>{user.name} {user.id === currentId && <span style={{ fontSize: 11, color: "var(--accent)" }}>(você)</span>}</strong>
                      <span>{user.email}</span>
                    </div>
                  </div>
                </td>
                <td>{catLabel(user.category)}</td>
                <td>{roleLabel(user.role)}</td>
                <td>
                  <span className={`status-badge ${user.active ? "active" : "inactive"}`}>
                    {user.active ? "Ativo" : "Inativo"}
                  </span>
                </td>
                <td>
                  <div className="row-actions" onClick={e => e.stopPropagation()}>
                    {(isAdmin || isManager) && (
                      <button className="btn btn-ghost btn-sm" onClick={() => { setSelected(user); setShowVacations(true); setShowEdit(false); }}>
                        Férias
                      </button>
                    )}
                    {isAdmin && (
                      <button className="btn btn-secondary btn-sm" onClick={() => { setSelected(user); openEdit(user); }}>
                        Editar
                      </button>
                    )}
                    <button className="btn btn-ghost btn-sm" onClick={() => navigate(`${routes.inbox}?dm=${user.id}`)}>
                      Mensagem
                    </button>
                  </div>
                </td>
              </tr>
            ))}
            {displayed.length === 0 && (
              <tr>
                <td colSpan={5} style={{ textAlign: "center", color: "var(--text-muted)", padding: "32px 16px" }}>
                  Nenhum funcionário encontrado.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {/* Detail panel */}
      {selected && !showEdit && !showVacations && (
        <>
          <div className="detail-overlay" onClick={() => setSelected(null)} />
          <div className="detail-panel">
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
              <div className="detail-panel-header">
                <UserAvatar name={selected.name} profilePicture={selected.profilePicture} size="lg" />
                <div className="detail-panel-name">
                  <h2>{selected.name}</h2>
                  <span>{selected.employeeNumber}</span>
                </div>
              </div>
              <button className="btn-icon" onClick={() => setSelected(null)}><X size={14} /></button>
            </div>

            <div className="detail-info-list">
              {(isAdmin || isManager) && (
                <>
                  <div className="detail-info-row"><span>Email</span><strong>{selected.email}</strong></div>
                  <div className="detail-info-row"><span>Contacto</span><strong>{selected.contact ?? "—"}</strong></div>
                </>
              )}
              <div className="detail-info-row"><span>Role</span><strong>{roleLabel(selected.role)}</strong></div>
              <div className="detail-info-row"><span>Categoria</span><strong>{catLabel(selected.category)}</strong></div>
              <div className="detail-info-row">
                <span>Estado</span>
                <strong>
                  <span className={`status-badge ${selected.active ? "active" : "inactive"}`}>
                    {selected.active ? "Ativo" : "Inativo"}
                  </span>
                </strong>
              </div>
            </div>

            <div className="detail-actions">
              <button className="btn btn-secondary btn-sm" onClick={() => navigate(routes.scheduler)}>
                Ver agenda
              </button>
              <button className="btn btn-secondary btn-sm" onClick={() => { setSelected(null); navigate(`${routes.inbox}?dm=${selected.id}`); }}>
                Mensagem
              </button>
              {(isAdmin || isManager) && (
                <button className="btn btn-ghost btn-sm" onClick={() => setShowVacations(true)}>
                  Ver férias
                </button>
              )}
              {isAdmin && (
                <button className="btn btn-ghost btn-sm" onClick={() => openEdit(selected)}>
                  Editar
                </button>
              )}
              {isAdmin && (
                <button
                  className={`btn btn-sm ${selected.active ? "btn-danger" : "btn-primary"}`}
                  onClick={() => void toggleActive(selected)}
                  disabled={isActLoading}
                  style={{ gridColumn: "span 2" }}
                >
                  {isActLoading ? "A processar..." : selected.active ? "Desativar conta" : "Reativar conta"}
                </button>
              )}
            </div>

            {error && <div className="error-banner">{error}</div>}
          </div>
        </>
      )}

      {/* Edit panel */}
      {selected && showEdit && (
        <>
          <div className="detail-overlay" onClick={() => { setShowEdit(false); setSelected(null); }} />
          <div className="detail-panel">
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <h2 style={{ fontSize: 18, fontWeight: 700 }}>Editar — {selected.name}</h2>
              <button className="btn-icon" onClick={() => setShowEdit(false)}><X size={14} /></button>
            </div>

            {error && <div className="error-banner">{error}</div>}

            <form onSubmit={handleEditSubmit} style={{ display: "flex", flexDirection: "column", gap: 0 }}>
              <div className="form-group">
                <label>Nome</label>
                <input value={editForm.name} onChange={e => setEditForm(f => ({ ...f, name: e.target.value }))} />
              </div>
              <div className="form-group">
                <label>Email</label>
                <input type="email" value={editForm.email} onChange={e => setEditForm(f => ({ ...f, email: e.target.value }))} />
              </div>
              <div className="form-group">
                <label>Contacto</label>
                <input value={editForm.contact} onChange={e => setEditForm(f => ({ ...f, contact: e.target.value }))} />
              </div>
              <div className="form-group">
                <label>Role</label>
                <select value={editForm.role} onChange={e => setEditForm(f => ({ ...f, role: e.target.value }))}>
                  <option value="EMPLOYEE">Funcionário</option>
                  <option value="MANAGER">Gerente</option>
                  <option value="ADMIN">Administrador</option>
                </select>
              </div>
              <div className="form-group">
                <label>Categoria</label>
                <select value={editForm.category} onChange={e => setEditForm(f => ({ ...f, category: e.target.value }))}>
                  <option value="VETERINARIAN">Veterinário</option>
                  <option value="NURSE">Enfermeiro</option>
                  <option value="ADMINISTRATIVE">Administrativo</option>
                  <option value="OPERATIONAL">Operacional</option>
                </select>
              </div>
              <div className="form-actions">
                <button type="button" className="btn btn-ghost btn-sm" onClick={() => setShowEdit(false)}>Cancelar</button>
                <button type="submit" className="btn btn-primary btn-sm" disabled={isSaving}>
                  {isSaving ? "A guardar..." : "Guardar"}
                </button>
              </div>
            </form>
          </div>
        </>
      )}

      {/* Vacations panel */}
      {selected && showVacations && (
        <>
          <div className="detail-overlay" onClick={() => { setShowVacations(false); setSelected(null); }} />
          <div className="detail-panel">
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <h2 style={{ fontSize: 18, fontWeight: 700 }}>Férias — {selected.name}</h2>
              <button className="btn-icon" onClick={() => setShowVacations(false)}><X size={14} /></button>
            </div>

            {userVacations.length === 0 ? (
              <p className="muted-text">Sem férias aprovadas.</p>
            ) : (
              <div className="detail-info-list">
                {userVacations.map(r => (
                  <div key={r.id} className="detail-info-row">
                    <span>{fmtDate(r.startDate)} — {fmtDate(r.endDate)}</span>
                    <button
                      className="btn btn-danger btn-sm"
                      onClick={() => void removeVacation(r.id)}
                    >
                      Remover
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </>
      )}

      {/* Toast */}
      {toast && (
        <div className="toast-container">
          <div className="toast"><Check size={15} /> {toast}</div>
        </div>
      )}
    </div>
  );
}
