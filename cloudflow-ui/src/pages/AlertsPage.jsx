import { useQuery } from '@tanstack/react-query';
import { dashboardApi } from '../api/dashboardApi';
import LoadingSpinner from '../components/shared/LoadingSpinner';
import { format } from 'date-fns';

export default function AlertsPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['alerts'],
    queryFn: () => dashboardApi.getAlerts().then((r) => r.data),
  });

  const alerts = data?.content || [];

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Alert Logs</h1>
      <div className="bg-white rounded-xl shadow-sm border">
        {isLoading ? <LoadingSpinner /> : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50">
                <tr>
                  {['Sent At','Webhook URL','HTTP Status','Delivered','Payload'].map((h) => (
                    <th key={h} className="text-left px-4 py-3 text-gray-600 font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {alerts.length === 0 ? (
                  <tr><td colSpan={5} className="text-center py-8 text-gray-400">No alerts yet.</td></tr>
                ) : alerts.map((alert) => (
                  <tr key={alert.id}>
                    <td className="px-4 py-3 text-gray-600">
                      {format(new Date(alert.sentAt), 'MMM d, HH:mm:ss')}
                    </td>
                    <td className="px-4 py-3 text-xs text-gray-500 max-w-xs truncate">{alert.webhookUrl}</td>
                    <td className="px-4 py-3 font-mono text-xs">{alert.httpStatus || '—'}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                        alert.isDelivered
                          ? 'bg-green-100 text-green-800'
                          : 'bg-red-100 text-red-800'
                      }`}>
                        {alert.isDelivered ? 'Yes' : 'No'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-xs text-gray-500 max-w-xs truncate font-mono">
                      {alert.payload}
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