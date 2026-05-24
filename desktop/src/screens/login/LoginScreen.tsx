import { FormEvent, useState } from "react";
import { login } from "../../api/authApi";
import { tokenManager } from "../../storage/tokenManager";

interface LoginScreenProps {
  onLoginSuccess: () => void;
}

export function LoginScreen({ onLoginSuccess }: LoginScreenProps) {
  const [employeeNumber, setEmployeeNumber] = useState("");
  const [password, setPassword] = useState("");
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function handleLogin(event?: FormEvent<HTMLFormElement>) {
    event?.preventDefault();

    if (employeeNumber.trim() === "" || password.trim() === "") {
      setErrorMessage("Por favor, preencha todos os campos");
      return;
    }

    try {
      setIsLoading(true);
      setErrorMessage(null);

      const response = await login({
        employeeNumber: employeeNumber.trim(),
        password
      });

      tokenManager.saveTokens(
        response.accessToken,
        response.refreshToken,
        response.user
      );

      onLoginSuccess();
    } catch {
      setErrorMessage("Credenciais inválidas ou erro de ligação ao servidor.");
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <main className="login-page">
      <form className="login-card" onSubmit={handleLogin}>
        <div className="login-header">
          <h1>🐾 MiauGenda</h1>
          <p>Sistema de Gestão de Turnos</p>
        </div>

        <label className="form-field">
          <span>Número de Funcionário</span>
          <input
            value={employeeNumber}
            onChange={(event) => {
              setEmployeeNumber(event.target.value);
              setErrorMessage(null);
            }}
            placeholder="Ex: FNC001"
            disabled={isLoading}
            autoComplete="username"
          />
        </label>

        <label className="form-field">
          <span>Password</span>
          <div className="password-row">
            <input
              value={password}
              onChange={(event) => {
                setPassword(event.target.value);
                setErrorMessage(null);
              }}
              placeholder="••••••••"
              type={passwordVisible ? "text" : "password"}
              disabled={isLoading}
              autoComplete="current-password"
            />

            <button
              type="button"
              className="password-toggle"
              onClick={() => setPasswordVisible((current) => !current)}
              disabled={isLoading}
            >
              {passwordVisible ? "Ocultar" : "Mostrar"}
            </button>
          </div>
        </label>

        {errorMessage && (
          <div className="error-box">
            {errorMessage}
          </div>
        )}

        <button className="primary-button" type="submit" disabled={isLoading}>
          {isLoading ? "A entrar..." : "Entrar"}
        </button>
      </form>
    </main>
  );
}