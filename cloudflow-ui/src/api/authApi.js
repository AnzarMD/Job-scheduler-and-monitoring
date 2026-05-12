import client from './client';

export const authApi = {
  register: (data) => client.post('/api/v1/auth/register', data),
  login: (data) => client.post('/api/v1/auth/login', data),
  logout: () => client.post('/api/v1/auth/logout'),
};