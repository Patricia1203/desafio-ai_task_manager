import { useState } from 'react';
import type { FormEvent } from 'react';
import type { Task, TaskInput, TaskPriority } from '../../types/task';
import { PRIORITY_LABELS, PRIORITY_OPTIONS } from '../../types/task';

interface TaskFormProps {
  task?: Task | null;
  submitting: boolean;
  onSubmit: (input: TaskInput) => void;
  onCancel: () => void;
  error?: string | null;
}

interface FieldErrors {
  titulo?: string;
  descricao?: string;
}

function validate(titulo: string, descricao: string): FieldErrors {
  const errors: FieldErrors = {};
  const trimmed = titulo.trim();
  if (!trimmed) {
    errors.titulo = 'Informe o título da tarefa.';
  } else if (trimmed.length > 200) {
    errors.titulo = 'O título pode ter no máximo 200 caracteres.';
  }
  if (descricao.length > 5000) {
    errors.descricao = 'A descrição pode ter no máximo 5000 caracteres.';
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
  const [titulo, setTitulo] = useState(task?.titulo ?? '');
  const [descricao, setDescricao] = useState(task?.descricao ?? '');
  const [prioridade, setPrioridade] = useState<TaskPriority>(task?.prioridade ?? 'MEDIA');
  const [prazo, setPrazo] = useState(task?.prazo ?? '');
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const errors = validate(titulo, descricao);
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }
    onSubmit({
      titulo: titulo.trim(),
      descricao: descricao.trim() || null,
      prioridade,
      prazo: prazo || null,
    });
  }

  return (
    <form className="task-form" onSubmit={handleSubmit} noValidate>
      <h3 className="task-form__title">{task ? 'Editar tarefa' : 'Nova tarefa'}</h3>

      <label htmlFor="task-form-titulo" className="field">
        <span className="field__label">Título *</span>
        <input
          id="task-form-titulo"
          value={titulo}
          maxLength={200}
          onChange={(event) => setTitulo(event.target.value)}
          aria-invalid={fieldErrors.titulo ? true : undefined}
        />
        {fieldErrors.titulo && (
          <span className="error-message" role="alert">
            {fieldErrors.titulo}
          </span>
        )}
      </label>

      <label htmlFor="task-form-descricao" className="field">
        <span className="field__label">Descrição</span>
        <textarea
          id="task-form-descricao"
          rows={4}
          value={descricao}
          maxLength={5000}
          onChange={(event) => setDescricao(event.target.value)}
          aria-invalid={fieldErrors.descricao ? true : undefined}
        />
        {fieldErrors.descricao && (
          <span className="error-message" role="alert">
            {fieldErrors.descricao}
          </span>
        )}
      </label>

      <label htmlFor="task-form-prioridade" className="field">
        <span className="field__label">Prioridade</span>
        <select
          id="task-form-prioridade"
          value={prioridade}
          onChange={(event) => setPrioridade(event.target.value as TaskPriority)}
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
          value={prazo}
          onChange={(event) => setPrazo(event.target.value)}
        />
      </label>

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
