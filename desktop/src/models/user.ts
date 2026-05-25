export interface User {
  id: number;
  employeeNumber: string;
  name: string;
  email: string;
  contact: string | null;
  profilePicture: string | null;
  role: string;
  category: string;
  active: boolean;
}

export interface UpdateProfileRequest {
  name: string;
  contact: string | null;
  password: string | null;
}

export interface CreateUserRequest {
  employeeNumber: string;
  name: string;
  email: string;
  contact?: string | null;
  password: string;
  role: string;
  category: string;
}

export interface UpdateUserRequest {
  name?: string | null;
  email?: string | null;
  contact?: string | null;
  role?: string | null;
  category?: string | null;
}