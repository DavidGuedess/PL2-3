import type { User } from "./user";

export interface LoginRequest {
  employeeNumber: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}