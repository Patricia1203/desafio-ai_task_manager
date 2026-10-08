import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { buscarConversa, enviarMensagem, listarConversas } from '../api/assistant';
import AssistantPage from '../pages/AssistantPage';

vi.mock('../api/assistant', () => ({
  enviarMensagem: vi.fn(),
  listarConversas: vi.fn(),
  buscarConversa: vi.fn(),
}));

function historico() {
  return within(screen.getByRole('navigation', { name: 'Histórico de conversas' }));
}

function janela() {
  return within(screen.getByRole('region', { name: 'Conversa' }));
}

describe('AssistantPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listarConversas).mockResolvedValue([]);
    vi.mocked(buscarConversa).mockResolvedValue({
      id: '00000000-0000-4000-8000-000000000000',
      title: '',
      updatedAt: '2026-10-08T10:00:00Z',
      messages: [],
    });
  });

  it('renderiza o cabecalho e o estado vazio', () => {
    render(<AssistantPage />);

    expect(screen.getByRole('heading', { name: 'Assistente' })).toBeInTheDocument();
    expect(screen.getByText(/Mande uma mensagem para consultar o assistente/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Nova conversa' })).toBeDisabled();
  });

  it('envia pela tecla Enter a primeira mensagem (sem conversationId) e exibe a resposta', async () => {
    vi.mocked(enviarMensagem).mockResolvedValue({
      conversationId: '0052e5f4-0000-4000-8000-000000000001',
      response: 'Voce tem 3 tarefas pendentes.',
    });
    render(<AssistantPage />);

    await userEvent.type(
      screen.getByLabelText('Sua mensagem'),
      'Quantas tarefas pendentes tenho?{Enter}',
    );

    expect(enviarMensagem).toHaveBeenCalledWith(
      null,
      'Quantas tarefas pendentes tenho?',
    );
    expect(await screen.findByText('Quantas tarefas pendentes tenho?')).toBeInTheDocument();
    expect(await screen.findByText('Voce tem 3 tarefas pendentes.')).toBeInTheDocument();
    expect(screen.getByText('Você')).toBeInTheDocument();
    expect(screen.getByText('Voce tem 3 tarefas pendentes.').closest('li')).toHaveTextContent(
      'Assistente',
    );
  });

  it('reutiliza o conversationId devolvido na mensagem seguinte', async () => {
    vi.mocked(enviarMensagem)
      .mockResolvedValueOnce({
        conversationId: '0052e5f4-0000-4000-8000-000000000001',
        response: 'Primeira resposta.',
      })
      .mockResolvedValueOnce({
        conversationId: '0052e5f4-0000-4000-8000-000000000001',
        response: 'Segunda resposta.',
      });
    render(<AssistantPage />);

    await userEvent.type(screen.getByLabelText('Sua mensagem'), 'tudo bem?');
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }));
    await screen.findByText('Primeira resposta.');

    await userEvent.type(screen.getByLabelText('Sua mensagem'), 'e agora?');
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }));
    await screen.findByText('Segunda resposta.');

    expect(enviarMensagem).toHaveBeenNthCalledWith(1, null, 'tudo bem?');
    expect(enviarMensagem).toHaveBeenNthCalledWith(
      2,
      '0052e5f4-0000-4000-8000-000000000001',
      'e agora?',
    );
    expect(screen.getByText('tudo bem?')).toBeInTheDocument();
    expect(screen.getByText('e agora?')).toBeInTheDocument();
    expect(screen.getByText('Primeira resposta.')).toBeInTheDocument();
    expect(screen.getByText('Segunda resposta.')).toBeInTheDocument();
  });

  it('nova conversa limpa o historico e zera o conversationId', async () => {
    vi.mocked(enviarMensagem).mockResolvedValue({
      conversationId: '0052e5f4-0000-4000-8000-000000000001',
      response: 'Resposta.',
    });
    render(<AssistantPage />);

    await userEvent.type(screen.getByLabelText('Sua mensagem'), 'oi');
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }));
    await screen.findByText('Resposta.');

    await userEvent.click(screen.getByRole('button', { name: 'Nova conversa' }));

    expect(screen.queryByText('oi')).not.toBeInTheDocument();
    expect(screen.queryByText('Resposta.')).not.toBeInTheDocument();
    expect(screen.getByText(/Mande uma mensagem para consultar o assistente/)).toBeInTheDocument();

    await userEvent.type(screen.getByLabelText('Sua mensagem'), 'de novo');
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }));
    await screen.findByText('Resposta.');

    expect(enviarMensagem).toHaveBeenLastCalledWith(null, 'de novo');
  });

  it('mostra erro do LLM em alert e mantem a mensagem do usuario', async () => {
    vi.mocked(enviarMensagem).mockRejectedValue(
      new Error('O servico de IA esta indisponivel no momento'),
    );
    render(<AssistantPage />);

    await userEvent.type(screen.getByLabelText('Sua mensagem'), 'posso consultar?');
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'O servico de IA esta indisponivel no momento',
    );
    expect(screen.getByText('posso consultar?')).toBeInTheDocument();
    expect(screen.queryByText(/Digitando/)).not.toBeInTheDocument();
    expect(screen.getByLabelText('Sua mensagem')).not.toBeDisabled();

    await userEvent.type(screen.getByLabelText('Sua mensagem'), 'outra pergunta');
    expect(screen.getByRole('button', { name: 'Enviar' })).not.toBeDisabled();
  });

  it('mostra o indicador de digitacao e desabilita entrada e botao durante a chamada', async () => {
    vi.mocked(enviarMensagem).mockReturnValue(new Promise(() => {}));
    render(<AssistantPage />);

    await userEvent.type(screen.getByLabelText('Sua mensagem'), 'quais?');
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }));

    expect(await screen.findByText('Digitando...')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Enviar' })).toBeDisabled();
    expect(screen.getByLabelText('Sua mensagem')).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Nova conversa' })).toBeDisabled();
  });

  it('nao envia mensagem em branco nem com so espacos', async () => {
    render(<AssistantPage />);

    const entrada = screen.getByLabelText('Sua mensagem');
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }));
    await userEvent.type(entrada, '   ');
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }));

    expect(enviarMensagem).not.toHaveBeenCalled();
    expect(screen.getByText(/Mande uma mensagem para consultar o assistente/)).toBeInTheDocument();
  });

  it('lista as conversas salvas no historico com titulo e data', async () => {
    vi.mocked(listarConversas).mockResolvedValue([
      {
        id: '0052e5f4-0000-4000-8000-000000000001',
        title: 'Quantas tarefas pendentes tenho?',
        updatedAt: '2026-10-08T10:00:00Z',
      },
      {
        id: '0052e5f4-0000-4000-8000-000000000002',
        title: 'Planejar a semana',
        updatedAt: '2026-10-07T09:00:00Z',
      },
    ]);
    render(<AssistantPage />);

    const nav = historico();
    expect(await nav.findByText('Quantas tarefas pendentes tenho?')).toBeInTheDocument();
    expect(nav.getByText('Planejar a semana')).toBeInTheDocument();
    expect(nav.getByText('08/10/2026')).toBeInTheDocument();
    expect(nav.getByText('07/10/2026')).toBeInTheDocument();
  });

  it('mostra estado vazio e fallback de titulo sem informacao no historico', async () => {
    render(<AssistantPage />);

    expect(await historico().findByText('Nenhuma conversa salva.')).toBeInTheDocument();
  });

  it('conversa sem titulo cai no fallback no historico', async () => {
    vi.mocked(listarConversas).mockResolvedValue([
      { id: '0052e5f4-0000-4000-8000-000000000001', title: '', updatedAt: '2026-10-08T10:00:00Z' },
    ]);
    render(<AssistantPage />);

    expect(await historico().findByText('Conversa')).toBeInTheDocument();
  });

  it('escolhe uma conversa do historico, restaura as mensagens e retoma o conversationId', async () => {
    const id = '0052e5f4-0000-4000-8000-000000000001';
    vi.mocked(listarConversas).mockResolvedValue([
      { id, title: 'O que tenho para hoje?', updatedAt: '2026-10-08T10:00:00Z' },
    ]);
    vi.mocked(buscarConversa).mockResolvedValue({
      id,
      title: 'O que tenho para hoje?',
      updatedAt: '2026-10-08T10:00:00Z',
      messages: [
        { role: 'user', content: 'O que tenho para hoje?' },
        { role: 'assistant', content: 'Voce tem uma tarefa vencida.' },
      ],
    });
    vi.mocked(enviarMensagem).mockResolvedValue({
      conversationId: id,
      response: 'Mais alguma coisa?',
    });
    render(<AssistantPage />);

    const nav = historico();
    await userEvent.click(await nav.findByRole('button', { name: /O que tenho para hoje/ }));

    expect(buscarConversa).toHaveBeenCalledWith(id);
    expect(screen.getByRole('button', { name: /O que tenho para hoje/ })).toHaveAttribute(
      'aria-current',
      'true',
    );
    const chat = janela();
    expect(await chat.findByText('O que tenho para hoje?')).toBeInTheDocument();
    expect(chat.getByText('Voce tem uma tarefa vencida.')).toBeInTheDocument();

    await userEvent.type(chat.getByLabelText('Sua mensagem'), 'e depois?{Enter}');

    expect(enviarMensagem).toHaveBeenCalledWith(id, 'e depois?');
    expect(await chat.findByText('Mais alguma coisa?')).toBeInTheDocument();
  });

  it('falha ao restaurar uma conversa mostra erro e mantem a janela atual', async () => {
    const id = '0052e5f4-0000-4000-8000-000000000001';
    vi.mocked(listarConversas).mockResolvedValue([
      { id, title: 'Antiga', updatedAt: '2026-10-08T10:00:00Z' },
    ]);
    vi.mocked(buscarConversa).mockRejectedValue(new Error('Conversa nao encontrada'));
    render(<AssistantPage />);

    const nav = historico();
    await userEvent.click(await nav.findByRole('button', { name: /Antiga/ }));

    const chat = janela();
    expect(await chat.findByRole('alert')).toHaveTextContent('Conversa nao encontrada');
    expect(chat.getByText(/Mande uma mensagem para consultar o assistente/)).toBeInTheDocument();
    expect(enviarMensagem).not.toHaveBeenCalled();
  });

  it('esconde e mostra a aba de historico pelo botao do cabecalho, mantendo o chat', async () => {
    vi.mocked(listarConversas).mockResolvedValue([
      {
        id: '0052e5f4-0000-4000-8000-000000000001',
        title: 'Conversa antiga',
        updatedAt: '2026-10-08T10:00:00Z',
      },
    ]);
    render(<AssistantPage />);

    expect(await historico().findByText('Conversa antiga')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Ocultar histórico' }));

    expect(
      screen.queryByRole('navigation', { name: 'Histórico de conversas' }),
    ).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Mostrar histórico' })).toHaveAttribute(
      'aria-expanded',
      'false',
    );
    expect(screen.getByRole('region', { name: 'Conversa' })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Mostrar histórico' }));

    expect(await historico().findByText('Conversa antiga')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ocultar histórico' })).toHaveAttribute(
      'aria-expanded',
      'true',
    );
  });
});