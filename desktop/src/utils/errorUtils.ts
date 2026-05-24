import { AxiosError } from "axios";

type BackendErrorData = {
  message?: string;
  error?: string;
  details?: string;
};

export function getBackendErrorMessage(
  error: unknown,
  fallback = "Ocorreu um erro inesperado."
): string {
  if (error instanceof AxiosError) {
    const data = error.response?.data as BackendErrorData | undefined;

    if (data?.message) {
      return data.message;
    }

    if (data?.error) {
      return data.error;
    }

    if (data?.details) {
      return data.details;
    }

    if (error.response?.status === 400) {
      return "Pedido inválido. Verifica os dados enviados.";
    }

    if (error.response?.status === 401) {
      return "Sessão expirada. Faz login novamente.";
    }

    if (error.response?.status === 403) {
      return "Não tens permissões para executar esta ação.";
    }

    if (error.response?.status === 404) {
      return "Recurso não encontrado.";
    }

    if (error.response?.status === 409) {
      return "Não foi possível concluir a ação porque existe um conflito.";
    }

    if (error.message) {
      return error.message;
    }
  }

  return fallback;
}