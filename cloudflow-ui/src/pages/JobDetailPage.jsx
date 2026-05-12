import { useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { jobsApi } from '../api/jobsApi';
import JobStatusBadge from '../components/jobs/JobStatusBadge';
import LoadingSpinner from '../components/shared/LoadingSpinner';
import { format } from 'date-fns';
import toast from 'react-hot-toast';

export default function JobDetailPage() {
  const { id } = useParams();
  const queryClient = useQueryClient();

  const { data: job, isLoading: jobLoading } = useQuery({
    queryKey: ['job', id],
    queryFn: () => jobsApi.getById(id).then((r) => r.data),
  });

  const { data: execData, isLoading: execLoading } = useQuery({
    queryKey: ['executions', id],
    queryFn: () => jobsApi.getExecutions(id).then((r) => r.data),
  });

  const triggerMutation = useMutation({
    mutationFn: () => jobsApi.trigger(id),
    onSuccess: () => {
      toast.success('Job triggered!');
      setTimeout(() => queryClient.invalidateQueries(['executions', id]), 2000);
    },
    onError: () => toast.error('Failed to trigger job'),
  });

  if (jobLoading) return <LoadingSpinner />;

  const executions = execData?.content || [];

  return (
    <div className="p-6 max-w-5xl">
      {/* Job header */}
      <div className="bg-white rounded-xl shadow-sm border p-6 mb-6">
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{job?.name}</h1>
            <p className="text-gray-500 text-sm mt-1">{job?.description || 'No description'}</p>
          </div>
          <div className="flex gap-3 items-center">
            <JobStatusBadge status={job?.status} />
            <button
              onClick={() => triggerMutation.mutate()}
              disabled={triggerMutation.isPending}
              className="px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              {triggerMutation.isPending ? 'Triggering...' : '▶ Run Now'}
            </button>
          </div>
        </div>

        <div className="grid grid-cols-3 gap-4 mt-6 pt-4 border-t text-sm">
          {[
            { label: 'Cron Expression', value: job?.cronExpression, mono: true },
            { label: 'Target URL', value: job?.targetUrl },
            { label: 'HTTP Method', value: job?.httpMethod },
            { label: 'Timeout', value: `${job?.timeoutSeconds}s` },
            { label: 'Retry Limit', value: job?.retryLimit },
            { label: 'Timezone', value: job?.timezone },
          ].map(({ label, value, mono }) => (
            <div key={label}>
              <p className="text-gray-500">{label}</p>
              <p className={`font-medium mt-0.5 ${mono ? 'font-mono text-xs' : ''}`}>{value}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Execution history */}
      <div className="bg-white rounded-xl shadow-sm border">
        <div className="p-4 border-b">
          <h2 className="font-semibold text-gray-900">Execution History</h2>
        </div>
        {execLoading ? <LoadingSpinner /> : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50">
                <tr>
                  {['Status','Attempt','Started At','Duration','HTTP Status','Error'].map((h) => (
                    <th key={h} className="text-left px-4 py-3 text-gray-600 font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {executions.length === 0 ? (
                  <tr><td colSpan={6} className="text-center py-8 text-gray-400">
                    No executions yet. Click "Run Now" to trigger the job.
                  </td></tr>
                ) : executions.map((exec) => (
                  <tr key={exec.id}>
                    <td className="px-4 py-3"><JobStatusBadge status={exec.status} /></td>
                    <td className="px-4 py-3 text-gray-600">#{exec.attemptNumber}</td>
                    <td className="px-4 py-3 text-gray-600">
                      {format(new Date(exec.startedAt), 'MMM d, HH:mm:ss')}
                    </td>
                    <td className="px-4 py-3 text-gray-600">
                      {exec.durationMs != null ? `${exec.durationMs}ms` : '—'}
                    </td>
                    <td className="px-4 py-3">
                      {exec.httpStatusCode ? (
                        <span className={`font-mono text-xs ${
                          exec.httpStatusCode < 300 ? 'text-green-600' : 'text-red-600'
                        }`}>
                          {exec.httpStatusCode}
                        </span>
                      ) : '—'}
                    </td>
                    <td className="px-4 py-3 text-xs text-red-500 max-w-xs truncate">
                      {exec.errorMessage || '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}