import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { enviarMensagem } from '../api/assistant';
import AssistantPage from '../pages/AssistantPage';

vi.mock('../api/assistant', () => ({
  enviarMensagem: vi.fn(),
}));

describe('AssistantPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
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
});