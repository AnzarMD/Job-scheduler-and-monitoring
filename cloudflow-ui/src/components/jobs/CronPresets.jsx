const PRESETS = [
  { label: 'Every minute',       value: '0 * * * * ?' },
  { label: 'Every 5 minutes',    value: '0 */5 * * * ?' },
  { label: 'Every hour',         value: '0 0 * * * ?' },
  { label: 'Every day at midnight', value: '0 0 0 * * ?' },
  { label: 'Every day at 9 AM',  value: '0 0 9 * * ?' },
  { label: 'Every Monday 9 AM',  value: '0 0 9 ? * MON' },
  { label: 'Every 1st of month', value: '0 0 0 1 * ?' },
];

export default function CronPresets({ onSelect }) {
  return (
    <div className="flex flex-wrap gap-2 mt-1">
      {PRESETS.map((preset) => (
        <button
          key={preset.value}
          type="button"
          onClick={() => onSelect(preset.value)}
          className="px-2 py-1 text-xs bg-gray-100 hover:bg-blue-100 hover:text-blue-700 rounded border border-gray-200 transition-colors"
        >
          {preset.label}
        </button>
      ))}
    </div>
  );
}