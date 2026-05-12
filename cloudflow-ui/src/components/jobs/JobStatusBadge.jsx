const statusConfig = {
  ACTIVE:   { color: 'bg-green-100 text-green-800',  label: 'Active' },
  PAUSED:   { color: 'bg-yellow-100 text-yellow-800', label: 'Paused' },
  DISABLED: { color: 'bg-gray-100 text-gray-800',    label: 'Disabled' },
  RUNNING:  { color: 'bg-blue-100 text-blue-800',    label: 'Running' },
  SUCCESS:  { color: 'bg-green-100 text-green-800',  label: 'Success' },
  FAILED:   { color: 'bg-red-100 text-red-800',      label: 'Failed' },
  TIMEOUT:  { color: 'bg-orange-100 text-orange-800',label: 'Timeout' },
};

export default function JobStatusBadge({ status }) {
  const config = statusConfig[status] || { color: 'bg-gray-100 text-gray-800', label: status };
  return (
    <span className={`px-2 py-1 rounded-full text-xs font-medium ${config.color}`}>
      {config.label}
    </span>
  );
}