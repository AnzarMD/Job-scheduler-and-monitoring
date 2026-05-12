import client from './client';

export const jobsApi = {
  getAll: (page = 0, size = 20) =>
    client.get(`/api/v1/jobs?page=${page}&size=${size}`),
  getById: (id) => client.get(`/api/v1/jobs/${id}`),
  create: (data) => client.post('/api/v1/jobs', data),
  update: (id, data) => client.put(`/api/v1/jobs/${id}`, data),
  delete: (id) => client.delete(`/api/v1/jobs/${id}`),
  pause: (id) => client.post(`/api/v1/jobs/${id}/pause`),
  resume: (id) => client.post(`/api/v1/jobs/${id}/resume`),
  trigger: (id) => client.post(`/api/v1/jobs/${id}/trigger`),
  getExecutions: (id, page = 0, size = 20) =>
    client.get(`/api/v1/jobs/${id}/executions?page=${page}&size=${size}`),
  getAlertConfig: (id) => client.get(`/api/v1/jobs/${id}/alert-config`),
  updateAlertConfig: (id, data) => client.put(`/api/v1/jobs/${id}/alert-config`, data),
};