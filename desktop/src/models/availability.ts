export interface Availability {
  id: number;
  userId: number;
  date: string;
  type: string;
  note: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateAvailabilityBody {
  date: string;
  type: string;
  note?: string | null;
}