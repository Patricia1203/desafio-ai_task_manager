import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import AppLayout from '../components/layout/AppLayout';

describe('AppLayout', () => {
  it('exibe as quatro rotas principais na navegacao', () => {
    render(
      <MemoryRouter>
        <AppLayout />
      </MemoryRouter>,
    );

    const nav = screen.getByLabelText('Navegacao principal');
    expect(nav).toHaveTextContent('Dashboard');
    expect(nav).toHaveTextContent('Tarefas');
    expect(nav).toHaveTextContent('Quadros');
    expect(nav).toHaveTextContent('Assistente');
  });
});