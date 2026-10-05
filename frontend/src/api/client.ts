export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api';

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  traceId?: string;
  timestamp?: string;
  errors?: Record<string, string>;
  code?: string;
}

export class ApiError extends Error {
  readonly status: number;
  readonly problem: ProblemDetail;

  constructor(status: number, problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `Erro ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.problem = problem;
  }
}

function buildMessage(problem: ProblemDetail, status: number): string {
  if (problem.errors && Object.keys(problem.errors).length > 0) {
    const fields = Object.entries(problem.errors)
      .map(([field, message]) => `${field}: ${message}`)
      .join('; ');
    return fields;
  }
  return problem.detail ?? problem.title ?? `Erro inesperado (${status})`;
}

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      Accept: 'application/json',
      ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
      ...init?.headers,
    },
    ...init,
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  const payload: unknown = text ? JSON.parse(text) : {};

  if (!response.ok) {
    const problem = payload as ProblemDetail;
    throw new ApiError(response.status, {
      ...problem,
      detail: buildMessage(problem, response.status),
    });
  }

  return payload as T;
}