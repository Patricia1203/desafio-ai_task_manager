import type { TaskStatus } from '../../types/task';
import { STATUS_LABELS, STATUS_OPTIONS } from '../../types/task';

interface StatusSelectProps {
  id: string;
  value: TaskStatus;
  onChange: (status: TaskStatus) => void;
  disabled?: boolean;
}

export default function StatusSelect({ id, value, onChange, disabled }: StatusSelectProps) {
  return (
    <label htmlFor={id} className="field">
      <span className="field__label">Status</span>
      <select
        id={id}
        value={value}
        disabled={disabled}
        onChange={(event) => onChange(event.target.value as TaskStatus)}
      >
        {STATUS_OPTIONS.map((status) => (
          <option key={status} value={status}>
            {STATUS_LABELS[status]}
          </option>
        ))}
      </select>
    </label>
  );
}
