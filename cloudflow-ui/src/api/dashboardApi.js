import client from './client';

export const dashboardApi = {
  getAllExecutions: (page = 0, size = 20) =>
    client.get(`/api/v1/executions?page=${page}&size=${size}`),
  getAlerts: (page = 0, size = 20) =>
    client.get(`/api/v1/alerts?page=${page}&size=${size}`),
};