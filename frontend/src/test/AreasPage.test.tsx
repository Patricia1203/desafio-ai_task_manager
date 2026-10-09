import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createArea, deleteArea, listAreas, updateArea } from '../api/areas';
import AreasPage from '../pages/AreasPage';

vi.mock('../api/areas', () => ({
  areaImageUrl: (id: string) => `/api/areas/${id}/image`,
  createArea: vi.fn(),
  deleteArea: vi.fn(),
  listAreas: vi.fn(),
  updateArea: vi.fn(),
}));

const areas = [
  { id: '3f1d3f6e-0000-4000-8000-000000000001', title: 'Moradia', imageType: null },
  { id: '3f1d3f6e-0000-4000-8000-000000000002', title: 'Finanças', imageType: 'image/png' },
];

function montar() {
  return render(
    <MemoryRouter>
      <AreasPage />
    </MemoryRouter>,
  );
}

describe('AreasPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listAreas).mockResolvedValue(areas);
    vi.mocked(createArea).mockResolvedValue(areas[0]);
    vi.mocked(updateArea).mockResolvedValue(areas[0]);
    vi.mocked(deleteArea).mockResolvedValue(undefined);
  });

  it('lista as areas como cards com nome e foto', async () => {
    montar();

    expect(await screen.findByRole('link', { name: /Moradia/ })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Finanças/ })).toBeInTheDocument();
    expect(screen.getByAltText('')).toHaveAttribute(
      'src',
      '/api/areas/3f1d3f6e-0000-4000-8000-000000000002/image',
    );
  });

  it('cria uma area enviando FormData com o nome', async () => {
    montar();
    await screen.findByRole('link', { name: /Moradia/ });

    await userEvent.click(screen.getByRole('button', { name: 'Novo quadro' }));
    await userEvent.type(screen.getByLabelText('Nome *'), '  Estudos  ');
    await userEvent.click(screen.getByRole('button', { name: 'Salvar' }));

    expect(createArea).toHaveBeenCalledTimes(1);
    const form = vi.mocked(createArea).mock.calls[0]?.[0] as FormData;
    expect(form.get('title')).toBe('Estudos');
  });

  it('edita a area e pode marcar remover foto', async () => {
    montar();
    const cardFinancas = (await screen.findAllByRole('listitem')).find((card) =>
      within(card).queryByText('Finanças'),
    );
    await userEvent.click(within(cardFinancas as HTMLElement).getByRole('button', { name: 'Editar' }));
    await userEvent.click(screen.getByRole('checkbox', { name: 'Remover foto' }));
    await userEvent.click(screen.getByRole('button', { name: 'Salvar' }));

    expect(updateArea).toHaveBeenCalledTimes(1);
    const form = vi.mocked(updateArea).mock.calls[0]?.[1] as FormData;
    expect(form.get('title')).toBe('Finanças');
    expect(form.get('removeImage')).toBe('true');
  });

  it('exclui a area com aviso de que as tarefas sao preservadas', async () => {
    montar();
    const cardMoradia = (await screen.findAllByRole('listitem')).find((card) =>
      within(card).queryByText('Moradia'),
    );
    await userEvent.click(
      within(cardMoradia as HTMLElement).getByRole('button', { name: 'Excluir' }),
    );

    const dialogo = screen.getByRole('alertdialog');
    expect(within(dialogo).getByText(/não serão apagadas/)).toBeInTheDocument();
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Excluir' }));

    expect(deleteArea).toHaveBeenCalledWith('3f1d3f6e-0000-4000-8000-000000000001');
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
  });

  it('busca quadro pelo titulo consultando o backend com o termo', async () => {
    montar();
    await screen.findByRole('link', { name: /Moradia/ });

    await userEvent.type(screen.getByLabelText('Buscar quadro pelo título'), 'morad');

    expect(listAreas).toHaveBeenLastCalledWith({ title: 'morad' });
  });

  it('mostra o breadcrumb de Quadros sem Dashboard', async () => {
    montar();
    await screen.findByRole('link', { name: /Moradia/ });

    const trilha = screen.getByLabelText('Trilha de navegação');
    expect(trilha).toHaveTextContent('Quadros');
    expect(trilha).not.toHaveTextContent('Dashboard');
  });

  it('recusa foto acima de 5MB antes de enviar', async () => {
    montar();
    await screen.findByRole('link', { name: /Moradia/ });
    await userEvent.click(screen.getByRole('button', { name: 'Novo quadro' }));

    const grande = new File(['x'], 'grande.png', { type: 'image/png' });
    Object.defineProperty(grande, 'size', { value: 5 * 1024 * 1024 + 1 });
    await userEvent.upload(screen.getByLabelText(/Foto/), grande);

    expect(await screen.findByText(/no máximo 5MB/)).toBeInTheDocument();
    expect(createArea).not.toHaveBeenCalled();
  });
});