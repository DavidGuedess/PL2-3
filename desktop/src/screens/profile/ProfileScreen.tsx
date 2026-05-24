import { FormEvent, useEffect, useState } from "react";
import { User as UserIcon, Calendar, Mail, Bell, LogOut, ChevronRight } from "lucide-react";
import { useNavigate } from "react-router-dom";

import { getMe, updateMe } from "../../api/userApi";
import type { User } from "../../models/user";
import { routes } from "../../navigation/routes";
import { tokenManager } from "../../storage/tokenManager";

function initials(name: string) {
  return name.split(" ").filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join("") || "?";
}

function catLabel(c: string) {
  if (c === "VETERINARIAN")  return "Veterinario";
  if (c === "NURSE")         return "Enfermeiro";
  if (c === "ADMINISTRATIVE") return "Administrativo";
  if (c === "OPERATIONAL")   return "Operacional";
  return c;
}

function roleLabel(r: string) {
  if (r === "ADMIN")   return "Administrador";
  if (r === "MANAGER") return "Gerente";
  return "Funcionario";
}

export function ProfileScreen() {
  const navigate = useNavigate();

  const [user,    setUser]    = useState<User | null>(null);
  const [subScreen, setSub]   = useState<"menu" | "edit">("menu");

  // edit form
  const [name,     setName]    = useState("");
  const [contact,  setContact] = useState("");
  const [password, setPassword] = useState("");

  const [emailNotif, setEmailNotif] = useState(true);
  const [pushNotif,  setPushNotif]  = useState(true);

  const [isLoading, setIsLoading] = useState(false);
  const [isSaving,  setIsSaving]  = useState(false);
  const [error,     setError]     = useState<string | null>(null);
  const [toast,     setToast]     = useState<string | null>(null);

  const fallbackName  = tokenManager.getUserName() ?? "Utilizador";
  const displayName   = user?.name ?? fallbackName;

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  }

  async function loadProfile() {
    setIsLoading(true); setError(null);
    try {
      const u = await getMe();
      setUser(u);
      setName(u.name);
      setContact(u.contact ?? "");
    } catch {
      setError("Erro ao carregar perfil");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleSave(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) { setError("O nome nao pode estar vazio"); return; }
    setIsSaving(true); setError(null);
    try {
      const updated = await updateMe({
        name:     name.trim(),
        contact:  contact.trim() || null,
        password: password.trim() || null,
      });
      setUser(updated);
      setName(updated.name);
      setContact(updated.contact ?? "");
      setPassword("");
      tokenManager.updateUserData(updated.name, updated.category);
      setSub("menu");
      showToast("Perfil atualizado.");
    } catch {
      setError("Erro ao atualizar perfil");
    } finally {
      setIsSaving(false);
    }
  }

  function handleLogout() {
    tokenManager.clearTokens();
    navigate(routes.login, { replace: true });
  }

  useEffect(() => { void loadProfile(); }, []);

  if (subScreen === "edit") {
    return (
      <div style={{ maxWidth: 520 }}>
        <div className="page-header">
          <h1>Editar Perfil</h1>
          <button className="btn btn-ghost btn-sm" onClick={() => { setSub("menu"); setError(null); }}>Cancelar</button>
        </div>

        {error && <div className="error-banner">{error}</div>}

        <div className="panel-card">
          <div style={{ display: "flex", justifyContent: "center", marginBottom: 24 }}>
            <div className="user-avatar lg">{initials(name || displayName)}</div>
          </div>

          <form onSubmit={handleSave} style={{ display: "flex", flexDirection: "column", gap: 0 }}>
            <div className="form-group">
              <label>Nome completo</label>
              <input value={name} onChange={e => setName(e.target.value)} disabled={isSaving} placeholder="Nome completo" />
            </div>
            <div className="form-group">
              <label>Contacto</label>
              <input value={contact} onChange={e => setContact(e.target.value)} disabled={isSaving} placeholder="Contacto (opcional)" />
            </div>
            <div className="form-group">
              <label>Categoria</label>
              <input value={user ? catLabel(user.category) : ""} disabled />
            </div>
            <div className="form-group">
              <label>Nova password</label>
              <input
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                disabled={isSaving}
                placeholder="Deixar em branco para manter"
              />
            </div>
            <div className="form-actions">
              <button type="button" className="btn btn-ghost btn-sm" onClick={() => { setSub("menu"); setError(null); }}>
                Cancelar
              </button>
              <button type="submit" className="btn btn-primary btn-sm" disabled={isSaving || !name.trim()}>
                {isSaving ? "A guardar..." : "Guardar"}
              </button>
            </div>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 520 }}>
      <div className="page-header">
        <h1>Perfil</h1>
      </div>

      {isLoading ? (
        <div className="loading-state">A carregar...</div>
      ) : (
        <>
          {/* Identity card */}
          <div className="panel-card" style={{ marginBottom: 20, display: "flex", alignItems: "center", gap: 20 }}>
            <div className="user-avatar lg">{initials(displayName)}</div>
            <div>
              <div style={{ fontSize: 18, fontWeight: 700, color: "var(--text)" }}>{displayName}</div>
              {user && (
                <div style={{ fontSize: 13, color: "var(--text-muted)", marginTop: 3 }}>
                  {roleLabel(user.role)} · {catLabel(user.category)}
                </div>
              )}
              {user?.employeeNumber && (
                <div style={{ fontSize: 12, color: "var(--text-faint)", marginTop: 2 }}>{user.employeeNumber}</div>
              )}
            </div>
          </div>

          {/* Info card */}
          {user && (
            <div className="detail-info-list" style={{ marginBottom: 20 }}>
              <div className="detail-info-row"><span>Email</span><strong>{user.email}</strong></div>
              <div className="detail-info-row"><span>Contacto</span><strong>{user.contact ?? "—"}</strong></div>
              <div className="detail-info-row"><span>Numero</span><strong>{user.employeeNumber}</strong></div>
              <div className="detail-info-row"><span>Role</span><strong>{roleLabel(user.role)}</strong></div>
              <div className="detail-info-row"><span>Categoria</span><strong>{catLabel(user.category)}</strong></div>
            </div>
          )}

          {/* Actions */}
          <div className="panel-card" style={{ marginBottom: 20, display: "flex", flexDirection: "column", gap: 2, padding: "8px 0" }}>
            <button
              className="sidebar-item"
              onClick={() => setSub("edit")}
              style={{ padding: "12px 20px" }}
            >
              <UserIcon size={16} className="sidebar-item-icon" />
              <span>Atualizar Perfil</span>
              <ChevronRight size={15} style={{ marginLeft: "auto", color: "var(--text-faint)" }} />
            </button>
            <button
              className="sidebar-item"
              onClick={() => navigate(routes.availability)}
              style={{ padding: "12px 20px" }}
            >
              <Calendar size={16} className="sidebar-item-icon" />
              <span>A Minha Disponibilidade</span>
              <ChevronRight size={15} style={{ marginLeft: "auto", color: "var(--text-faint)" }} />
            </button>
          </div>

          {/* Preferences */}
          <div className="panel-card" style={{ marginBottom: 20, padding: 0 }}>
            <div style={{ padding: "10px 20px 6px", borderBottom: "1px solid var(--border)" }}>
              <span style={{ fontSize: 11, fontWeight: 700, color: "var(--text-muted)", textTransform: "uppercase", letterSpacing: "0.6px" }}>
                Preferencias
              </span>
            </div>
            <label className="sidebar-item" style={{ padding: "12px 20px", cursor: "pointer" }}>
              <Mail size={16} className="sidebar-item-icon" />
              <span>Notificacoes por Email</span>
              <input
                type="checkbox"
                checked={emailNotif}
                onChange={e => setEmailNotif(e.target.checked)}
                style={{ marginLeft: "auto", width: 16, height: 16, accentColor: "var(--accent)" }}
              />
            </label>
            <label className="sidebar-item" style={{ padding: "12px 20px", cursor: "pointer" }}>
              <Bell size={16} className="sidebar-item-icon" />
              <span>Notificacoes Push</span>
              <input
                type="checkbox"
                checked={pushNotif}
                onChange={e => setPushNotif(e.target.checked)}
                style={{ marginLeft: "auto", width: 16, height: 16, accentColor: "var(--accent)" }}
              />
            </label>
          </div>

          {/* Logout */}
          <div className="panel-card" style={{ padding: 0 }}>
            <button
              className="sidebar-item"
              onClick={handleLogout}
              style={{ padding: "12px 20px", color: "var(--red)", width: "100%" }}
            >
              <LogOut size={16} className="sidebar-item-icon" />
              <span>Terminar Sessao</span>
            </button>
          </div>
        </>
      )}

      {toast && (
        <div className="toast-container">
          <div className="toast">✓ {toast}</div>
        </div>
      )}
    </div>
  );
}
