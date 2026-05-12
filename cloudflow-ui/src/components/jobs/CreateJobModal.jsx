import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { jobsApi } from '../../api/jobsApi';
import CronPresets from './CronPresets';
import toast from 'react-hot-toast';

export default function CreateJobModal({ onClose }) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState({
    name: '', description: '', cronExpression: '', timezone: 'UTC',
    targetUrl: '', httpMethod: 'GET', requestBody: '',
    timeoutSeconds: 30, retryLimit: 3, retryDelaySeconds: 60,
  });

  const set = (field) => (e) =>
    setForm((prev) => ({ ...prev, [field]: e.target.value }));

  const mutation = useMutation({
    mutationFn: () => jobsApi.create(form),
    onSuccess: () => {
      queryClient.invalidateQueries(['jobs']);
      toast.success('Job created successfully!');
      onClose();
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Failed to create job');
    },
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    mutation.mutate();
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-lg font-semibold">Create New Job</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-2xl">×</button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Job Name *</label>
            <input value={form.name} onChange={set('name')} required
              className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Nightly Sales Export" />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Cron Expression *</label>
            <input value={form.cronExpression} onChange={set('cronExpression')} required
              className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="0 0 23 * * ?" />
            <CronPresets onSelect={(val) => setForm((p) => ({ ...p, cronExpression: val }))} />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Target URL *</label>
            <input value={form.targetUrl} onChange={set('targetUrl')} required
              className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="https://api.example.com/export" />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">HTTP Method</label>
              <select value={form.httpMethod} onChange={set('httpMethod')}
                className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                {['GET','POST','PUT','PATCH','DELETE'].map((m) => (
                  <option key={m}>{m}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Timezone</label>
              <input value={form.timezone} onChange={set('timezone')}
                className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
          </div>

          {form.httpMethod !== 'GET' && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Request Body (JSON)</label>
              <textarea value={form.requestBody} onChange={set('requestBody')} rows={3}
                className="w-full border rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder='{"key": "value"}' />
            </div>
          )}

          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Timeout (s)</label>
              <input type="number" value={form.timeoutSeconds} onChange={set('timeoutSeconds')}
                className="w-full border rounded-lg px-3 py-2 text-sm" min={1} max={300} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Retry Limit</label>
              <input type="number" value={form.retryLimit} onChange={set('retryLimit')}
                className="w-full border rounded-lg px-3 py-2 text-sm" min={0} max={10} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Retry Delay (s)</label>
              <input type="number" value={form.retryDelaySeconds} onChange={set('retryDelaySeconds')}
                className="w-full border rounded-lg px-3 py-2 text-sm" min={0} max={3600} />
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t">
            <button type="button" onClick={onClose}
              className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">
              Cancel
            </button>
            <button type="submit" disabled={mutation.isPending}
              className="px-6 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 disabled:opacity-50">
              {mutation.isPending ? 'Creating...' : 'Create Job'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}