import type { User } from "../models/user";

const KEY_ACCESS_TOKEN = "access_token";
const KEY_REFRESH_TOKEN = "refresh_token";
const KEY_USER_ID = "user_id";
const KEY_USER_NAME = "user_name";
const KEY_USER_EMAIL = "user_email";
const KEY_USER_ROLE = "user_role";
const KEY_USER_CATEGORY = "user_category";
const KEY_EMPLOYEE_NUMBER = "employee_number";

export const tokenManager = {
  saveTokens(accessToken: string, refreshToken: string, user: User): void {
    localStorage.setItem(KEY_ACCESS_TOKEN, accessToken);
    localStorage.setItem(KEY_REFRESH_TOKEN, refreshToken);
    localStorage.setItem(KEY_USER_ID, String(user.id));
    localStorage.setItem(KEY_USER_NAME, user.name);
    localStorage.setItem(KEY_USER_EMAIL, user.email);
    localStorage.setItem(KEY_USER_ROLE, user.role);
    localStorage.setItem(KEY_USER_CATEGORY, user.category);
    localStorage.setItem(KEY_EMPLOYEE_NUMBER, user.employeeNumber);
  },

  getAccessToken(): string | null {
    return localStorage.getItem(KEY_ACCESS_TOKEN);
  },

  getRefreshToken(): string | null {
    return localStorage.getItem(KEY_REFRESH_TOKEN);
  },

  getUserId(): number {
    const value = localStorage.getItem(KEY_USER_ID);
    return value ? Number(value) : -1;
  },

  getUserName(): string | null {
    return localStorage.getItem(KEY_USER_NAME);
  },

  getUserEmail(): string | null {
    return localStorage.getItem(KEY_USER_EMAIL);
  },

  getUserRole(): string | null {
    return localStorage.getItem(KEY_USER_ROLE);
  },

  getUserCategory(): string | null {
    return localStorage.getItem(KEY_USER_CATEGORY);
  },

  getEmployeeNumber(): string | null {
    return localStorage.getItem(KEY_EMPLOYEE_NUMBER);
  },

  isLoggedIn(): boolean {
    return this.getAccessToken() !== null;
  },

  clearTokens(): void {
    localStorage.removeItem(KEY_ACCESS_TOKEN);
    localStorage.removeItem(KEY_REFRESH_TOKEN);
    localStorage.removeItem(KEY_USER_ID);
    localStorage.removeItem(KEY_USER_NAME);
    localStorage.removeItem(KEY_USER_EMAIL);
    localStorage.removeItem(KEY_USER_ROLE);
    localStorage.removeItem(KEY_USER_CATEGORY);
    localStorage.removeItem(KEY_EMPLOYEE_NUMBER);
  },

  updateAccessToken(accessToken: string): void {
    localStorage.setItem(KEY_ACCESS_TOKEN, accessToken);
  },

  updateTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem(KEY_ACCESS_TOKEN, accessToken);
    localStorage.setItem(KEY_REFRESH_TOKEN, refreshToken);
  },

  updateUserData(name: string, category: string): void {
    localStorage.setItem(KEY_USER_NAME, name);
    localStorage.setItem(KEY_USER_CATEGORY, category);
  }
};