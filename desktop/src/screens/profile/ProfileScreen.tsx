import { FormEvent, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { getMe, updateMe } from "../../api/userApi";
import type { User } from "../../models/user";
import { routes } from "../../navigation/routes";
import { tokenManager } from "../../storage/tokenManager";

type ProfileSubScreen = "menu" | "update";

function getInitials(name: string): string {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join("") || "?";
}

function formatCategory(category: string): string {
  switch (category) {
    case "VETERINARIAN":
      return "Veterinário";
    case "NURSE":
      return "Enfermeiro";
    case "ADMINISTRATIVE":
      return "Administrativo";
    case "OPERATIONAL":
      return "Operacional";
    default:
      return category;
  }
}

function formatRole(role: string): string {
  switch (role) {
    case "ADMIN":
      return "Administrador";
    case "MANAGER":
      return "Gerente";
    case "EMPLOYEE":
      return "Funcionário";
    default:
      return role;
  }
}

export function ProfileScreen() {
  const navigate = useNavigate();

  const [subScreen, setSubScreen] = useState<ProfileSubScreen>("menu");
  const [user, setUser] = useState<User | null>(null);

  const [name, setName] = useState("");
  const [contact, setContact] = useState("");
  const [password, setPassword] = useState("");

  const [emailNotifications, setEmailNotifications] = useState(true);
  const [pushNotifications, setPushNotifications] = useState(true);

  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const fallbackName = tokenManager.getUserName() ?? "Utilizador";
  const displayName = user?.name ?? fallbackName;

  async function loadProfile() {
    try {
      setIsLoading(true);
      setErrorMessage(null);
      setSuccessMessage(null);

      const currentUser = await getMe();

      setUser(currentUser);
      setName(currentUser.name);
      setContact(currentUser.contact ?? "");
    } catch {
      setErrorMessage("Erro ao carregar perfil");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (name.trim() === "") {
      setErrorMessage("O nome não pode estar vazio");
      return;
    }

    try {
      setIsSaving(true);
      setErrorMessage(null);
      setSuccessMessage(null);

      const updatedUser = await updateMe({
        name: name.trim(),
        contact: contact.trim() === "" ? null : contact.trim(),
        password: password.trim() === "" ? null : password
      });

      setUser(updatedUser);
      setName(updatedUser.name);
      setContact(updatedUser.contact ?? "");
      setPassword("");

      tokenManager.updateUserData(updatedUser.name, updatedUser.category);

      setSuccessMessage("Perfil atualizado com sucesso");
    } catch {
      setErrorMessage("Erro ao atualizar perfil");
    } finally {
      setIsSaving(false);
    }
  }

  function handleLogout() {
    tokenManager.clearTokens();
    navigate(routes.login, { replace: true });
  }

  useEffect(() => {
    void loadProfile();
  }, []);

  useEffect(() => {
    if (!successMessage) {
      return;
    }

    const timeout = window.setTimeout(() => {
      setSuccessMessage(null);
    }, 3000);

    return () => {
      window.clearTimeout(timeout);
    };
  }, [successMessage]);

  if (subScreen === "update") {
    return (
      <main className="profile-page">
        {successMessage && (
          <div className="success-banner">
            {successMessage}
          </div>
        )}

        <section className="profile-shell">
          <header className="profile-topbar">
            <button
              className="text-button"
              onClick={() => {
                setSubScreen("menu");
                setErrorMessage(null);
                setSuccessMessage(null);
              }}
            >
              Cancelar
            </button>

            <strong>{name || "Perfil"}</strong>

            <button
              className="text-button"
              form="profile-form"
              type="submit"
              disabled={isSaving || name.trim() === ""}
            >
              {isSaving ? "..." : "Guardar"}
            </button>
          </header>

          <form id="profile-form" className="profile-form" onSubmit={handleSave}>
            <div className="profile-avatar-wrapper">
              <div className="profile-avatar large">
                {getInitials(name || displayName)}
              </div>
              <div className="camera-dot">📷</div>
            </div>

            {isLoading && (
              <p className="muted-text center-text">A carregar perfil...</p>
            )}

            <section className="profile-form-card">
              <label>
                <span>Nome completo</span>
                <input
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  disabled={isSaving}
                  placeholder="Nome completo"
                />
              </label>

              <label>
                <span>Contacto</span>
                <input
                  value={contact}
                  onChange={(event) => setContact(event.target.value)}
                  disabled={isSaving}
                  placeholder="Contacto"
                />
              </label>

              <label>
                <span>Categoria</span>
                <input
                  value={user ? formatCategory(user.category) : ""}
                  disabled
                  placeholder="Categoria"
                />
              </label>

              <label>
                <span>Nova password</span>
                <input
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  disabled={isSaving}
                  placeholder="Nova password (opcional)"
                  type="password"
                />
              </label>
            </section>

            {errorMessage && (
              <div className="profile-error">
                {errorMessage}
              </div>
            )}
          </form>
        </section>
      </main>
    );
  }

  return (
    <main className="profile-page">
      <section className="profile-shell">
        <header className="profile-back-header">
          <button className="secondary-button" onClick={() => navigate(routes.dashboard)}>
            Voltar
          </button>
        </header>

        <section className="profile-identity">
          <div className="profile-avatar">
            {getInitials(displayName)}
          </div>

          <h1>{displayName}</h1>

          {user && (
            <p>
              {formatRole(user.role)} · {formatCategory(user.category)}
            </p>
          )}
        </section>

        {errorMessage && (
          <div className="profile-error">
            {errorMessage}
          </div>
        )}

        <section className="profile-menu-card">
          <button className="profile-menu-row" onClick={() => setSubScreen("update")}>
            <span className="menu-icon">👤</span>
            <span>Atualizar Perfil</span>
            <strong>›</strong>
          </button>

          <button
            className="profile-menu-row"
            onClick={() => navigate(routes.availability)}
          >
            <span className="menu-icon">📅</span>
            <span>A Minha Disponibilidade</span>
            <strong>›</strong>
          </button>
        </section>

        <h2 className="profile-section-title">Preferências</h2>

        <section className="profile-menu-card">
          <label className="profile-menu-row toggle-row">
            <span className="menu-icon">✉️</span>
            <span>Notificações por Email</span>

            <input
              type="checkbox"
              checked={emailNotifications}
              onChange={(event) => setEmailNotifications(event.target.checked)}
            />
          </label>

          <label className="profile-menu-row toggle-row">
            <span className="menu-icon">🔔</span>
            <span>Notificações Push</span>

            <input
              type="checkbox"
              checked={pushNotifications}
              onChange={(event) => setPushNotifications(event.target.checked)}
            />
          </label>
        </section>

        <section className="profile-menu-card danger-section">
          <button className="profile-menu-row logout-row" onClick={handleLogout}>
            <span className="menu-icon danger-icon">↪</span>
            <span>Terminar Sessão</span>
          </button>
        </section>
      </section>
    </main>
  );
}