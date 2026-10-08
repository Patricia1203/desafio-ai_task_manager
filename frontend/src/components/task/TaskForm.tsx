import { useState } from 'react';
import type { FormEvent } from 'react';
import type { Task, TaskInput, TaskPriority, TaskTimeUnit } from '../../types/task';
import { PRIORITY_LABELS, PRIORITY_OPTIONS, TIME_UNIT_LABELS, TIME_UNIT_OPTIONS } from '../../types/task';

interface TaskFormProps {
  task?: Task | null;
  submitting: boolean;
  onSubmit: (input: TaskInput) => void;
  onCancel: () => void;
  error?: string | null;
}

interface FieldErrors {
  title?: string;
  description?: string;
  estimatedTime?: string;
}

function validate(title: string, description: string, estimatedTime: string): FieldErrors {
  const errors: FieldErrors = {};
  const trimmed = title.trim();
  if (!trimmed) {
    errors.title = 'Informe o título da tarefa.';
  } else if (trimmed.length > 200) {
    errors.title = 'O título pode ter no máximo 200 caracteres.';
  }
  if (description.length > 5000) {
    errors.description = 'A descrição pode ter no máximo 5000 caracteres.';
  }
  const tempo = estimatedTime.trim();
  if (tempo) {
    const numero = Number(tempo);
    if (!Number.isFinite(numero) || numero <= 0 || numero > 200) {
      errors.estimatedTime = 'O tempo estimado deve ser maior que zero e até 200.';
    }
  }
  return errors;
}

export default function TaskForm({
  task,
  submitting,
  onSubmit,
  onCancel,
  error,
}: TaskFormProps) {
  const [title, setTitle] = useState(task?.title ?? '');
  const [description, setDescription] = useState(task?.description ?? '');
  const [priority, setPriority] = useState<TaskPriority>(task?.priority ?? 'MEDIUM');
  const [dueDate, setDueDate] = useState(task?.dueDate ?? '');
  const [estimatedTime, setEstimatedTime] = useState(
    task?.estimatedTime != null ? String(task.estimatedTime) : '',
  );
  const [estimatedUnit, setEstimatedUnit] = useState<TaskTimeUnit>(
    task?.estimatedUnit ?? 'HOURS',
  );
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const errors = validate(title, description, estimatedTime);
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }
    const tempo = estimatedTime.trim();
    const numero = tempo ? Number(tempo) : null;
    onSubmit({
      title: title.trim(),
      description: description.trim() || null,
      priority,
      dueDate: dueDate || null,
      estimatedTime: numero,
      estimatedUnit: numero != null ? estimatedUnit : null,
    });
  }

  return (
    <form className="task-form" onSubmit={handleSubmit} noValidate>
      <h3 className="task-form__title">{task ? 'Editar tarefa' : 'Nova tarefa'}</h3>

      <label htmlFor="task-form-titulo" className="field">
        <span className="field__label">Título *</span>
        <input
          id="task-form-titulo"
          value={title}
          maxLength={200}
          onChange={(event) => setTitle(event.target.value)}
          aria-invalid={fieldErrors.title ? true : undefined}
        />
        {fieldErrors.title && (
          <span className="error-message" role="alert">
            {fieldErrors.title}
          </span>
        )}
      </label>

      <label htmlFor="task-form-descricao" className="field">
        <span className="field__label">Descrição</span>
        <textarea
          id="task-form-descricao"
          rows={4}
          value={description}
          maxLength={5000}
          onChange={(event) => setDescription(event.target.value)}
          aria-invalid={fieldErrors.description ? true : undefined}
        />
        {fieldErrors.description && (
          <span className="error-message" role="alert">
            {fieldErrors.description}
          </span>
        )}
      </label>

      <label htmlFor="task-form-prioridade" className="field">
        <span className="field__label">Prioridade</span>
        <select
          id="task-form-prioridade"
          value={priority}
          onChange={(event) => setPriority(event.target.value as TaskPriority)}
        >
          {PRIORITY_OPTIONS.map((option) => (
            <option key={option} value={option}>
              {PRIORITY_LABELS[option]}
            </option>
          ))}
        </select>
      </label>

      <label htmlFor="task-form-prazo" className="field">
        <span className="field__label">Prazo</span>
        <input
          id="task-form-prazo"
          type="date"
          value={dueDate}
          onChange={(event) => setDueDate(event.target.value)}
        />
      </label>

      <div className="task-form__linha">
        <label htmlFor="task-form-tempo" className="field">
          <span className="field__label">Tempo estimado</span>
          <input
            id="task-form-tempo"
            type="number"
            min="0"
            max="200"
            step="0.1"
            inputMode="decimal"
            value={estimatedTime}
            placeholder="Ex.: 4"
            onChange={(event) => setEstimatedTime(event.target.value)}
            aria-invalid={fieldErrors.estimatedTime ? true : undefined}
          />
          {fieldErrors.estimatedTime && (
            <span className="error-message" role="alert">
              {fieldErrors.estimatedTime}
            </span>
          )}
        </label>

        <label htmlFor="task-form-unidade" className="field">
          <span className="field__label">Unidade</span>
          <select
            id="task-form-unidade"
            value={estimatedUnit}
            onChange={(event) => setEstimatedUnit(event.target.value as TaskTimeUnit)}
          >
            {TIME_UNIT_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {TIME_UNIT_LABELS[option]}
              </option>
            ))}
          </select>
        </label>
      </div>

      {error && (
        <p className="error-message" role="alert">
          {error}
        </p>
      )}

      <div className="task-form__actions">
        <button type="submit" disabled={submitting}>
          {submitting ? 'Salvando...' : 'Salvar'}
        </button>
        <button type="button" onClick={onCancel} disabled={submitting}>
          Cancelar
        </button>
      </div>
    </form>
  );
}
