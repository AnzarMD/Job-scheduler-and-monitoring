import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { jobsApi } from '../api/jobsApi';
import { dashboardApi } from '../api/dashboardApi';
import JobStatusBadge from '../components/jobs/JobStatusBadge';
import CreateJobModal from '../components/jobs/CreateJobModal';
import { format } from 'date-fns';
import toast from 'react-hot-toast';

export default function DashboardPage() {
  const [showModal, setShowModal] = useState(false);

  const { data: jobsData, isLoading: jobsLoading, refetch: refetchJobs } = useQuery({
    queryKey: ['jobs'],
    queryFn: () => jobsApi.getAll(0, 50).then((r) => r.data),
  });

  const { data: execData } = useQuery({
    queryKey: ['recent-executions'],
    queryFn: () => dashboardApi.getAllExecutions(0, 100).then((r) => r.data),
  });

  const jobs = jobsData?.content || [];
  const executions = execData?.content || [];

  const totalJobs = jobsData?.totalElements || 0;
  const activeJobs = jobs.filter((j) => j.status === 'ACTIVE').length;
  const today = new Date().toDateString();
  const todayExecs = executions.filter(
    (e) => new Date(e.startedAt).toDateString() === today
  );
  const successToday = todayExecs.filter((e) => e.status === 'SUCCESS').length;
  const failedToday = todayExecs.filter((e) => e.status === 'FAILED').length;
  const successRate = todayExecs.length > 0
    ? Math.round((successToday / todayExecs.length) * 100)
    : 0;

  const handleTrigger = async (id, name) => {
    try {
      await jobsApi.trigger(id);
      toast.success(`Job "${name}" triggered!`);
      setTimeout(refetchJobs, 1000);
    } catch {
      toast.error('Failed to trigger job');
    }
  };

  const handlePauseResume = async (job) => {
    try {
      if (job.status === 'ACTIVE') {
        await jobsApi.pause(job.id);
        toast.success(`Job "${job.name}" paused`);
      } else {
        await jobsApi.resume(job.id);
        toast.success(`Job "${job.name}" resumed`);
      }
      refetchJobs();
    } catch {
      toast.error('Failed to update job status');
    }
  };

  return (
    <div className="p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-gray-500 text-sm mt-1">Monitor your scheduled jobs</p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 text-sm font-medium"
        >
          + New Job
        </button>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-4 gap-4 mb-8">
        {[
          { label: 'Total Jobs', value: totalJobs, color: 'text-blue-600' },
          { label: 'Active Jobs', value: activeJobs, color: 'text-green-600' },
          { label: 'Success Rate', value: `${successRate}%`, color: 'text-green-600' },
          { label: 'Failed Today', value: failedToday, color: 'text-red-600' },
        ].map((stat) => (
          <div key={stat.label} className="bg-white rounded-xl p-5 shadow-sm border">
            <p className="text-sm text-gray-500">{stat.label}</p>
            <p className={`text-3xl font-bold mt-1 ${stat.color}`}>{stat.value}</p>
          </div>
        ))}
      </div>

      {/* Jobs table */}
      <div className="bg-white rounded-xl shadow-sm border">
        <div className="p-4 border-b">
          <h2 className="font-semibold text-gray-900">All Jobs</h2>
        </div>

        {/* CHANGED: loading skeleton instead of LoadingSpinner */}
        {jobsLoading ? (
          <div className="p-4 space-y-3">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="h-16 bg-gray-100 rounded-lg animate-pulse" />
            ))}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50">
                <tr>
                  {['Name', 'Status', 'Cron', 'Target URL', 'Last Run', 'Actions'].map((h) => (
                    <th key={h} className="text-left px-4 py-3 text-gray-600 font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">

                {/* CHANGED: empty state as valid <tr><td> — fixes invalid HTML,
                    and uses setShowModal (not setShowCreateModal) */}
                {jobs.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="text-center py-16">
                      <div className="text-6xl mb-4">📭</div>
                      <h3 className="text-lg font-semibold text-gray-700 mb-2">No jobs yet</h3>
                      <p className="text-gray-500 mb-6">Create your first scheduled job to get started.</p>
                      <button
                        onClick={() => setShowModal(true)}
                        className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700"
                      >
                        Create Your First Job
                      </button>
                    </td>
                  </tr>
                ) : (
                  jobs.map((job) => (
                    <tr key={job.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3 font-medium text-gray-900">{job.name}</td>
                      <td className="px-4 py-3"><JobStatusBadge status={job.status} /></td>
                      <td className="px-4 py-3 font-mono text-xs text-gray-500">{job.cronExpression}</td>
                      <td className="px-4 py-3 text-xs text-gray-500 max-w-xs truncate">{job.targetUrl}</td>
                      <td className="px-4 py-3 text-xs text-gray-500">
                        {job.lastExecutedAt
                          ? format(new Date(job.lastExecutedAt), 'MMM d, HH:mm')
                          : '—'}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex gap-2">
                          <button onClick={() => handleTrigger(job.id, job.name)}
                            className="px-2 py-1 text-xs bg-blue-50 text-blue-600 rounded hover:bg-blue-100">
                            ▶ Run
                          </button>
                          <button onClick={() => handlePauseResume(job)}
                            className="px-2 py-1 text-xs bg-gray-50 text-gray-600 rounded hover:bg-gray-100">
                            {job.status === 'ACTIVE' ? '⏸ Pause' : '▶ Resume'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}

              </tbody>
            </table>
          </div>
        )}
      </div>

      {showModal && <CreateJobModal onClose={() => setShowModal(false)} />}
    </div>
  );
}