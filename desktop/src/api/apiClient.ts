import axios, {
  AxiosError,
  AxiosResponse,
  InternalAxiosRequestConfig
} from "axios";

import { tokenManager } from "../storage/tokenManager";
import { signalTokenExpired } from "../state/authEvents";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:3001";

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

interface RefreshResponse {
  accessToken?: string;
  refreshToken?: string;
}

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    "Content-Type": "application/json"
  }
});

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenManager.getAccessToken();
  const url = config.url ?? "";
  const isAuthRoute = url.includes("/auth/");

  if (token && !isAuthRoute) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined;

    const isUnauthorized = error.response?.status === 401;
    const isAuthRoute = originalRequest?.url?.includes("/auth/") ?? false;

    if (
      isUnauthorized &&
      originalRequest &&
      !originalRequest._retry &&
      !isAuthRoute
    ) {
      originalRequest._retry = true;

      const refreshToken = tokenManager.getRefreshToken();

      if (!refreshToken) {
        tokenManager.clearTokens();
        signalTokenExpired();
        return Promise.reject(error);
      }

      try {
        const response = await axios.post<RefreshResponse>(
          `${API_BASE_URL}/api/auth/refresh`,
          { refreshToken }
        );

        const newAccessToken = response.data.accessToken;
        const newRefreshToken = response.data.refreshToken;

        if (!newAccessToken) {
          tokenManager.clearTokens();
          signalTokenExpired();
          return Promise.reject(error);
        }

        tokenManager.updateTokens(
          newAccessToken,
          newRefreshToken ?? refreshToken
        );

        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

        return apiClient(originalRequest);
      } catch (refreshError) {
        tokenManager.clearTokens();
        signalTokenExpired();
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);