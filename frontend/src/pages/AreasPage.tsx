import { useEffect, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { Link } from 'react-router-dom';
import type { WorkArea } from '../types/area';
import { areaImageUrl, createArea, deleteArea, listAreas, updateArea } from '../api/areas';
import AsyncState from '../components/common/AsyncState';

type Modo = 'lista' | 'form';

const TIPOS_ACEITOS = ['image/png', 'image/jpeg', 'image/webp', 'image/gif'];
const TAMANHO_MAXIMO_FOTO = 5 * 1024 * 1024;

function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : 'Erro inesperado.';
}

export default function AreasPage() {
  const [areas, setAreas] = useState<WorkArea[]>([]);
  const [tituloBusca, setTituloBusca] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [modo, setModo] = useState<Modo>('lista');
  const [editando, setEditando] = useState<WorkArea | null>(null);
  const [titulo, setTitulo] = useState('');
  const [arquivo, setArquivo] = useState<File | null>(null);
  const [removerFoto, setRemoverFoto] = useState(false);
  const [formBusy, setFormBusy] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [emExclusao, setEmExclusao] = useState<WorkArea | null>(null);
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const carregar = () => {
    setLoading(true);
    listAreas(tituloBusca.trim() ? { title: tituloBusca } : {})
      .then((lista) => {
        setAreas(lista);
        setError(null);
      })
      .catch((caught) => setError(messageOf(caught)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    let ativo = true;
    listAreas(tituloBusca.trim() ? { title: tituloBusca } : {})
      .then((lista) => {
        if (ativo) {
          setAreas(lista);
          setError(null);
        }
      })
      .catch((caught) => {
        if (ativo) setError(messageOf(caught));
      })
      .finally(() => {
        if (ativo) setLoading(false);
      });
    return () => {
      ativo = false;
    };
  }, [tituloBusca]);

  function abrirCriar() {
    setEditando(null);
    setTitulo('');
    setArquivo(null);
    setRemoverFoto(false);
    setFormError(null);
    setModo('form');
  }

  function abrirEditar(area: WorkArea) {
    setEditando(area);
    setTitulo(area.title);
    setArquivo(null);
    setRemoverFoto(false);
    setFormError(null);
    setModo('form');
  }

  /**
   * Valida a foto no cliente antes do upload: a whitelist e o teto de 5MB
   * espelham as regras do backend (WorkArea#setImage), poupando um round-trip
   * que terminaria em 422/413.
   */
  function selecionarFoto(event: ChangeEvent<HTMLInputElement>) {
    const escolhido = event.target.files?.[0] ?? null;
    if (!escolhido) {
      setArquivo(null);
      return;
    }
    if (!TIPOS_ACEITOS.includes(escolhido.type)) {
      setArquivo(null);
      event.target.value = '';
      setFormError('Formato de imagem não suportado. Use PNG, JPEG, WEBP ou GIF.');
      return;
    }
    if (escolhido.size > TAMANHO_MAXIMO_FOTO) {
      setArquivo(null);
      event.target.value = '';
      setFormError('A foto deve ter no máximo 5MB.');
      return;
    }
    setArquivo(escolhido);
    setFormError(null);
  }

  async function salvar(event: FormEvent) {
    event.preventDefault();
    const nome = titulo.trim();
    if (!nome) {
      setFormError('Informe o nome do quadro.');
      return;
    }
    const form = new FormData();
    form.set('title', nome);
    if (arquivo) {
      form.set('image', arquivo, arquivo.name);
    }
    if (editando && removerFoto) {
      form.set('removeImage', 'true');
    }
    setFormBusy(true);
    setFormError(null);
    try {
      if (editando) {
        await updateArea(editando.id, form);
      } else {
        await createArea(form);
      }
      setModo('lista');
      setEditando(null);
      carregar();
    } catch (caught) {
      setFormError(messageOf(caught));
    } finally {
      setFormBusy(false);
    }
  }

  async function confirmarExclusao() {
    if (!emExclusao) {
      return;
    }
    setDeleteBusy(true);
    setDeleteError(null);
    try {
      await deleteArea(emExclusao.id);
      setEmExclusao(null);
      carregar();
    } catch (caught) {
      setDeleteError(messageOf(caught));
    } finally {
      setDeleteBusy(false);
    }
  }

  return (
    <section aria-labelledby="areas-heading">
      <nav className="breadcrumb" aria-label="Trilha de navegação">
        {modo === 'form' ? (
          <>
            <button
              type="button"
              className="breadcrumb__link"
              onClick={() => setModo('lista')}
            >
              Quadros
            </button>
            <span className="breadcrumb__sep" aria-hidden="true">
              /
            </span>
            <span aria-current="page">{editando ? 'Editar quadro' : 'Novo quadro'}</span>
          </>
        ) : (
          <span aria-current="page">Quadros</span>
        )}
      </nav>

      <header className="tasks__header">
        <h2 id="areas-heading">Quadros</h2>
        {modo === 'lista' && (
          <button type="button" onClick={abrirCriar}>
            Novo quadro
          </button>
        )}
      </header>

      {modo === 'lista' && (
        <label htmlFor="areas-busca-titulo" className="field tasks__busca">
          <span className="field__label">Buscar quadro pelo título</span>
          <input
            id="areas-busca-titulo"
            type="search"
            placeholder="Ex.: moradia"
            value={tituloBusca}
            onChange={(event) => {
              setTituloBusca(event.target.value);
              setLoading(true);
            }}
          />
        </label>
      )}

      {modo === 'form' ? (
        <form className="task-form" onSubmit={salvar} noValidate>
          <h3 className="task-form__title">
            {editando ? `Editar quadro "${editando.title}"` : 'Novo quadro'}
          </h3>

          <label htmlFor="areas-form-nome" className="field">
            <span className="field__label">Nome *</span>
            <input
              id="areas-form-nome"
              value={titulo}
              maxLength={100}
              onChange={(event) => setTitulo(event.target.value)}
            />
          </label>

          <label htmlFor="areas-form-foto" className="field areas-form__foto">
            <span className="field__label">Foto</span>
            <input
              id="areas-form-foto"
              type="file"
              accept={TIPOS_ACEITOS.join(',')}
              onChange={selecionarFoto}
            />
            <span className="field__hint">PNG, JPEG, WEBP ou GIF, até 5MB.</span>
          </label>

          {editando?.imageType && !removerFoto && (
            <img
              className="areas-form__preview"
              src={areaImageUrl(editando.id)}
              alt={`Foto atual de ${editando.title}`}
            />
          )}

          {editando?.imageType && (
            <label className="field areas-form__remover" htmlFor="areas-form-remover-foto">
              <span className="field__label">Remover foto</span>
              <input
                id="areas-form-remover-foto"
                type="checkbox"
                checked={removerFoto}
                onChange={(event) => setRemoverFoto(event.target.checked)}
              />
            </label>
          )}

          {formError && (
            <p className="error-message" role="alert">
              {formError}
            </p>
          )}

          <div className="task-form__actions">
            <button type="submit" disabled={formBusy}>
              {formBusy ? 'Salvando...' : 'Salvar'}
            </button>
            <button type="button" onClick={() => setModo('lista')} disabled={formBusy}>
              Cancelar
            </button>
          </div>
        </form>
      ) : (
        <AsyncState
          loading={loading}
          error={error}
          onRetry={carregar}
          isEmpty={areas.length === 0}
          emptyMessage="Nenhum quadro cadastrado. Crie o primeiro!"
        >
          <ul className="areas__grade">
            {areas.map((area) => (
              <li key={area.id} className="areas__card">
                <Link className="areas__card-link" to={`/areas/${area.id}`}>
                  {area.imageType ? (
                    <img
                      className="areas__card-foto"
                      src={areaImageUrl(area.id)}
                      alt=""
                    />
                  ) : (
                    <span className="areas__card-foto areas__card-foto--vazia" aria-hidden="true" />
                  )}
                  <span className="areas__card-titulo">{area.title}</span>
                </Link>
                <div className="areas__card-acoes">
                  <button type="button" onClick={() => abrirEditar(area)}>
                    Editar
                  </button>
                  <button
                    type="button"
                    className="button--danger"
                    onClick={() => setEmExclusao(area)}
                  >
                    Excluir
                  </button>
                </div>
              </li>
            ))}
          </ul>
        </AsyncState>
      )}

      {emExclusao && (
        <div
          className="dialog"
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="dialogo-area-titulo"
        >
          <h4 id="dialogo-area-titulo">Excluir quadro?</h4>
          <p>
            O quadro <strong>{emExclusao.title}</strong> será excluído. As tarefas dele{' '}
            <strong>não serão apagadas</strong> — ficarão sem quadro.
          </p>
          {deleteError && (
            <p className="error-message" role="alert">
              {deleteError}
            </p>
          )}
          <div className="dialog__acoes">
            <button
              type="button"
              className="button--danger"
              onClick={confirmarExclusao}
              disabled={deleteBusy}
            >
              {deleteBusy ? 'Excluindo...' : 'Excluir'}
            </button>
            <button type="button" onClick={() => setEmExclusao(null)} disabled={deleteBusy}>
              Cancelar
            </button>
          </div>
        </div>
      )}
    </section>
  );
}