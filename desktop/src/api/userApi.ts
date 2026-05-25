import { apiClient } from "./apiClient";
import type {
  CreateUserRequest,
  UpdateProfileRequest,
  UpdateUserRequest,
  User
} from "../models/user";

export async function getUsers(): Promise<User[]> {
  const response = await apiClient.get<User[]>("/users");
  return response.data;
}

export async function getMe(): Promise<User> {
  const response = await apiClient.get<User>("/users/me");
  return response.data;
}

export async function updateMe(request: UpdateProfileRequest): Promise<User> {
  const response = await apiClient.patch<User>("/users/me", request);
  return response.data;
}

export async function createUser(request: CreateUserRequest): Promise<User> {
  const response = await apiClient.post<User>("/users", request);
  return response.data;
}

export async function updateUser(id: number, request: UpdateUserRequest): Promise<User> {
  const response = await apiClient.patch<User>(`/users/${id}`, request);
  return response.data;
}

export async function deactivateUser(id: number): Promise<User> {
  const response = await apiClient.patch<User>(`/users/${id}/deactivate`);
  return response.data;
}

export async function activateUser(id: number): Promise<User> {
  const response = await apiClient.patch<User>(`/users/${id}/activate`);
  return response.data;
}

export async function uploadAvatar(file: File): Promise<User> {
  const form = new FormData();
  form.append("avatar", file);
  const response = await apiClient.patch<User>("/users/me/avatar", form, {
    headers: { "Content-Type": "multipart/form-data" }
  });
  return response.data;
}