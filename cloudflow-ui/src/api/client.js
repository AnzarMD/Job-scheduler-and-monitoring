import axios from 'axios';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' }
});

// REQUEST interceptor — runs before every API call
// Reads the token from Zustand store (which persists to localStorage)
// and attaches it as Authorization: Bearer <token>
client.interceptors.request.use((config) => {
  // Import inline to avoid circular dependency
  const token = localStorage.getItem('cloudflow_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// RESPONSE interceptor — runs after every API response
// If the server returns 401 (token expired or blacklisted),
// clear auth state and redirect to login
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('cloudflow_token');
      localStorage.removeItem('cloudflow_user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default client;