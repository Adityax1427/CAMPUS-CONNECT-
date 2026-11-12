const API_URL = import.meta.env.VITE_API_URL || '';

export async function request(path, { token, ...options } = {}) {
  let response;
  try {
    response = await fetch(`${API_URL}/api${path}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options?.headers,
      },
    });
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error('Can’t connect to the CampusConnect server. Start the backend from the campus-connect folder with: cd backend; mvn spring-boot:run "-Dspring-boot.run.profiles=local"');
    }
    throw error;
  }

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || body.error || `Request failed (${response.status})`);
  }

  if (response.status === 204) return null;
  return response.json();
}

export const authApi = {
  login: (email, password) =>
    request('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  register: (name, email, password) =>
    request('/auth/register', { method: 'POST', body: JSON.stringify({ name, email, password }) }),
  me: (token) => request('/auth/me', { token }),
};
